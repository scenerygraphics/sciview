package sc.iview.zebrahub;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.blosc.JBlosc;
import org.blosc.PrimitiveSizes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class RemoteZarrLoader {

    private final String zarrUrl;
    private final Map<Integer, String> scaleUrls;
    private final int[][] dimensions; // Spatial dimensions for each scale level
    private final int[][] chunkSizes; // Chunk sizes for each scale level

    public RemoteZarrLoader(String zarrUrl) throws IOException {
        this.zarrUrl = zarrUrl;
        this.scaleUrls = fetchScaleUrls();
        this.dimensions = fetchDimensions();
        this.chunkSizes = fetchChunkSizes();
    }

    private Map<Integer, String> fetchScaleUrls() throws IOException {
        Map<Integer, String> scaleUrls = new HashMap<>();
        for (int i = 0; i < 3; i++) { // Assuming 3 scales
            scaleUrls.put(i, zarrUrl + "/" + i);
        }
        return scaleUrls;
    }

    private int[][] fetchDimensions() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        int[][] dims = new int[scaleUrls.size()][];

        for (Map.Entry<Integer, String> entry : scaleUrls.entrySet()) {
            URL url = new URL(entry.getValue() + "/.zarray");
            JsonNode root = mapper.readTree(openStream(url));
            JsonNode shape = root.get("shape");
            // Extract only the spatial dimensions: z, y, x
            dims[entry.getKey()] = new int[]{shape.get(2).asInt(), shape.get(3).asInt(), shape.get(4).asInt()};
        }
        return dims;
    }

    private int[][] fetchChunkSizes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        int[][] sizes = new int[scaleUrls.size()][];

        for (Map.Entry<Integer, String> entry : scaleUrls.entrySet()) {
            URL url = new URL(entry.getValue() + "/.zarray");
            JsonNode root = mapper.readTree(openStream(url));
            JsonNode chunks = root.get("chunks");
            // Extract only the spatial chunk sizes: z, y, x
            sizes[entry.getKey()] = new int[]{chunks.get(2).asInt(), chunks.get(3).asInt(), chunks.get(4).asInt()};
        }
        return sizes;
    }

    public int[][] getDimensions() {
        return dimensions;
    }

    public int[][] getChunkSizes() {
        return chunkSizes;
    }

    public VolatileShortArray loadData(int scale, int[] cellDims, long[] cellMin) throws IOException {
        String scaleUrl = scaleUrls.get(scale);
        if (scaleUrl == null) throw new IllegalArgumentException("Invalid scale level");

        // Convert cell dimensions and cell minimums to 5D, with hardcoded timepoint and channel values
        long[] chunkCoords = calculateChunkCoordinates(scale, cellMin);
        String chunkUrl = constructChunkUrl(scaleUrl, chunkCoords);
        byte[] compressedData = fetchChunkData(chunkUrl);

        // Decompress data using JBLosc
        byte[] dataBytes = decompressData(compressedData, cellDims);

        int size = cellDims[0] * cellDims[1] * cellDims[2];
        int expectedSize = size * 2; // 2 bytes per short

        if (dataBytes.length != expectedSize) {
            throw new IOException("Fetched data size does not match expected size. Expected: " + expectedSize + " but got: " + dataBytes.length);
        }

        VolatileShortArray shortArray = new VolatileShortArray(size, true);
        short[] data = shortArray.getCurrentStorageArray();

        ByteBuffer byteBuffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN); // Assuming little-endian
        byteBuffer.asShortBuffer().get(data);

        return shortArray;
    }

    private long[] calculateChunkCoordinates(int scale, long[] cellMin) {
        int[] chunkSizes = this.chunkSizes[scale];
        long[] chunkCoords = new long[5];
        chunkCoords[0] = 0; // Assuming single timepoint
        chunkCoords[1] = 0; // Assuming single channel
        chunkCoords[2] = cellMin[0] / chunkSizes[0];
        chunkCoords[3] = cellMin[1] / chunkSizes[1];
        chunkCoords[4] = cellMin[2] / chunkSizes[2];
        return chunkCoords;
    }

    private String constructChunkUrl(String scaleUrl, long[] chunkCoords) {
        // Construct URL based on the chunk coordinates
        // This logic depends on the Zarr chunking scheme and dimension separator "/"
        StringBuilder url = new StringBuilder(scaleUrl);
        for (long coord : chunkCoords) {
            url.append("/").append(coord);
        }
        return url.toString();
    }

    private byte[] fetchChunkData(String chunkUrl) throws IOException {
        URL url = new URL(chunkUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/octet-stream");
        connection.setDoInput(true);
        connection.connect();

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private byte[] decompressData(byte[] compressedData, int[] cellDims) throws IOException {
        JBlosc jb = new JBlosc();
        try {
            int decompressedSize = cellDims[0] * cellDims[1] * cellDims[2] * PrimitiveSizes.SHORT_FIELD_SIZE; // 2 bytes per short
            ByteBuffer compressedBuffer = ByteBuffer.allocateDirect(compressedData.length);
            compressedBuffer.put(compressedData);
            compressedBuffer.flip();

            ByteBuffer decompressedBuffer = ByteBuffer.allocateDirect(decompressedSize);

            jb.decompress(compressedBuffer, decompressedBuffer, decompressedSize);
            jb.destroy();

            byte[] decompressedData = new byte[decompressedSize];
            decompressedBuffer.get(decompressedData);
            return decompressedData;
        } catch (Exception e) {
            jb.destroy();
            throw new IOException("Failed to decompress data", e);
        }
    }

    private InputStream openStream(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoInput(true);
        connection.connect();
        return connection.getInputStream();
    }
}

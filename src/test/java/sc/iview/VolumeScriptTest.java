package sc.iview;

import org.scijava.command.CommandService;
import org.scijava.script.ScriptService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

public class VolumeScriptTest {

    static String script = "# @SciView sv\n" +
"# @UIService ui\n\n" +
"import net.imglib2.img.array.ArrayImgs as ArrayImgs\n" +
"w = 100\n" +
"h = w\n" +
"d = w\n" +
"img = ArrayImgs.unsignedBytes(w, h, d)\n" +
"img_ra = img.randomAccess()\n" +
"for x in range(0, w):\n" +
"  for y in range(0, h):\n" +
"    for z in range(0, d):\n" +
"      img_ra.setPosition([x, y, z])\n" +
"      x0 = x - 50\n" +
"      y0 = y - 50\n" +
"      z0 = z - 50\n" +
"      val = 255 - (x0**2 + y0**2 + z0**2) ** 0.65\n" +
"      img_ra.get().setReal(val)\n" +
"ui.show(img)\n" +
"sv.addVolume(img)\n";

     public static void main(String... args) throws Exception {
         SciView sv = SciView.create();

         ScriptService scriptService = sv.getScijavaContext().getService(ScriptService.class);

         File file = File.createTempFile("sciview-script", ".py");
         file.deleteOnExit();

         BufferedWriter writer = new BufferedWriter(new FileWriter(file));
         writer.write(script);
         writer.close();

         scriptService.run(file, true, new HashMap<String, Object>());
         scriptService.run(file, true, new HashMap<String, Object>());


    }

}

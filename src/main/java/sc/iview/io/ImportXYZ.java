package sc.iview.io;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.io.File;

/**
 * Created by kharrington on 7/20/17.
 */
@Plugin(type = Command.class,
        menuPath = "SciView>Import>Xyz")
public class ImportXYZ  implements Command {

    @Parameter
    private File xyzFile;

    @Parameter
    SciView sciView;

    @Override
    public void run() {
        if (xyzFile != null) {
            try {
                sciView.addXyz(xyzFile.getAbsolutePath());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package sc.iview.test;

import graphics.scenery.SceneryBase;
import io.scif.SCIFIOService;
import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;
import org.scijava.service.SciJavaService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import sc.iview.SciView;
import sc.iview.SciViewService;
import sc.iview.commands.demo.ResourceLoader;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Future;

public class AllScripts {

    public static void main(String[] args) {

        SceneryBase.xinitThreads();

        System.setProperty( "scijava.log.level:sc.iview", "debug" );
        Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class, ScriptService.class);

        UIService ui = context.service( UIService.class );
        if( !ui.isVisible() ) ui.showUI();

        IOService io = context.service( IOService.class );
        OpService ops = context.service( OpService.class );

        ScriptService scriptService = context.service( ScriptService.class );

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        HashMap<String,Object> testMap = new HashMap<>();
        testMap.put("sciView", sciView);
        try {
            File scriptFile = ResourceLoader.createFile( AllScripts.class, "/scripts/sphere_test.py" );
            File targetFile = ResourceLoader.createFile( AllScripts.class, "/outputs/sphere_test.png" );
            Img<UnsignedByteType> targetOutput = (Img<UnsignedByteType>) io.open(targetFile.getAbsolutePath());

            // Run script and block until done
            Future<ScriptModule> res = scriptService.run(scriptFile, true, testMap);
            while(!res.isDone()) {
                Thread.sleep(20);
            }

            Img<UnsignedByteType> scriptOutput = sciView.getScreenshot();

            IterableInterval<UnsignedByteType> diff = ops.math().subtract(targetOutput, (IterableInterval<UnsignedByteType>) scriptOutput);
            RealType sumDiff = ops.stats().sum(diff);
            System.out.println("Sum diff: " + sumDiff);

            //ui.show("Diff", diff);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}

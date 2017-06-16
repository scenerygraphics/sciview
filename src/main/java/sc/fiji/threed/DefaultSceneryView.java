package sc.fiji.threed;

import cleargl.GLVector;
import net.imagej.Data;
import net.imagej.Position;
import net.imagej.axis.AxisType;
import net.imagej.display.DataView;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/16/17.
 */
@Plugin(type = DataView.class)
public class DefaultSceneryView implements SceneryView {

    private boolean selected;
    private Context context;

    @Override
    public RealLocalizable getCameraPosition() {
        final Scenery scenery = getScenery();
        GLVector vpos = scenery.getViewer().getCamera().getPosition();
        return new RealPoint( vpos.x(), vpos.y(), vpos.z() );
    }

    @Override
    public Scenery getScenery() {
        return null;
    }

    @Override
    public boolean isCompatible(Data data) {
        return false;
    }

    @Override
    public void initialize(Data data) {

    }

    @Override
    public Data getData() {
        return getScenery();
    }

    @Override
    public Position getPlanePosition() {
        return null;
    }

    @Override
    public void setSelected(boolean isSelected) {
        this.selected = isSelected;
    }

    @Override
    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public int getPreferredWidth() {
        return getScenery().getViewer().getWindowWidth();
    }

    @Override
    public int getPreferredHeight() {
        return getScenery().getViewer().getWindowHeight();
    }

    @Override
    public void update() {
        /* TODO */
    }

    @Override
    public void rebuild() {
        /* TODO */
    }

    @Override
    public void dispose() {
        getScenery().getViewer().dispose();
    }

    @Override
    public int getIntPosition(AxisType axis) {
        return 0;
    }

    @Override
    public long getLongPosition(AxisType axis) {
        return 0;
    }

    @Override
    public void setPosition(long position, AxisType axis) {

    }

    @Override
    public void localize(int[] position) {

    }

    @Override
    public void localize(long[] position) {

    }

    @Override
    public int getIntPosition(int d) {
        return 0;
    }

    @Override
    public long getLongPosition(int d) {
        return 0;
    }

    @Override
    public void fwd(int d) {

    }

    @Override
    public void bck(int d) {

    }

    @Override
    public void move(int distance, int d) {

    }

    @Override
    public void move(long distance, int d) {

    }

    @Override
    public void move(Localizable localizable) {

    }

    @Override
    public void move(int[] distance) {

    }

    @Override
    public void move(long[] distance) {

    }

    @Override
    public void setPosition(Localizable localizable) {

    }

    @Override
    public void setPosition(int[] position) {

    }

    @Override
    public void setPosition(long[] position) {

    }

    @Override
    public void setPosition(int position, int d) {

    }

    @Override
    public void setPosition(long position, int d) {

    }

    @Override
    public void localize(float[] position) {
        getScenery().getViewer().moveCamera( position );
    }

    @Override
    public void localize(double[] position) {
        getScenery().getViewer().moveCamera( position );
    }

    @Override
    public float getFloatPosition(int d) {
        switch (d) {
            case 0: return getScenery().getViewer().getCamera().getPosition().x();
            case 1: return getScenery().getViewer().getCamera().getPosition().y();
            case 2: return getScenery().getViewer().getCamera().getPosition().z();
        }
        return 0;
    }

    @Override
    public double getDoublePosition(int d) {
        return getFloatPosition(d);
    }

    @Override
    public int numDimensions() {
        return 3;
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public Context getContext() {
        return context();
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }
}

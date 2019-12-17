package sc.iview.ui;

import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptREPL;
import org.scijava.ui.swing.script.OutputPane;
import org.scijava.ui.swing.script.VarsPane;
import org.scijava.widget.UIComponent;

import javax.script.ScriptContext;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A Swing UI pane for the SciJava scripting REPL.
 *
 * @author Curtis Rueden
 * @author Johannes Schindelin
 * @author Ulrik Guenther
 */
public class REPLPane implements UIComponent<JComponent> {

  private final ScriptREPL repl;

  private final JSplitPane mainPane;

  private final OutputPane output;
  private final REPLEditor prompt;
  private final VarsPane vars;

  @Parameter(required = false)
  private LogService log;

  /**
   * Constructs an interpreter UI pane for a SciJava scripting REPL.
   *
   * @param context The SciJava application context to use
   */
  public REPLPane(final Context context) {
    context.inject(this);
    output = new OutputPane(log);
    final JScrollPane outputScroll = new JScrollPane(output);
    outputScroll.setPreferredSize(new Dimension(440, 400));

    repl = new ScriptREPL(context, output.getOutputStream());
    repl.initialize();

    final Writer writer = output.getOutputWriter();
    final ScriptContext ctx = repl.getInterpreter().getEngine().getContext();
    ctx.setErrorWriter(writer);
    ctx.setWriter(writer);

    vars = new VarsPane(context, repl);
    vars.setBorder(new EmptyBorder(0, 0, 8, 0));

    prompt = new REPLEditor(repl, vars, output);
    context.inject(prompt);
    prompt.setREPLLanguage("Python");
    final JScrollPane promptScroll = new JScrollPane(prompt);

    final JPanel bottomPane = new JPanel();
    bottomPane.setLayout(new MigLayout("ins 0", "[grow,fill][pref]", "[grow,fill,align top]"));
    bottomPane.add(promptScroll, "spany 2");

    final JSplitPane outputAndPromptPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputScroll, bottomPane);
    outputAndPromptPane.setResizeWeight(1);
//    outputAndPromptPane.setDividerSize(2);

    mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vars,
            outputAndPromptPane);
    mainPane.setDividerSize(1);
    mainPane.setDividerLocation(0);
  }

  // -- InterpreterPane methods --

  /** Gets the associated script REPL. */
  public ScriptREPL getREPL() {
    return repl;
  }

  /** Prints a message to the output panel. */
  public void print(final String string) {
    final Writer writer = output.getOutputWriter();
    try {
      writer.write(string + "\n");
    }
    catch (final IOException e) {
      e.printStackTrace(new PrintWriter(writer));
    }
  }

  public void dispose() {
    output.close();
  }

  // -- UIComponent methods --

  @Override
  public JComponent getComponent() {
    return mainPane;
  }

  @Override
  public Class<JComponent> getComponentType() {
    return JComponent.class;
  }

}

/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.ui;

import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptREPL;
import org.scijava.ui.swing.script.OutputPane;
import org.scijava.widget.UIComponent;

import javax.script.ScriptContext;
import javax.swing.*;
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
 * @author Kyle Harrington
 */
public class REPLPane implements UIComponent<JComponent> {

  private final ScriptREPL repl;

  private final JSplitPane mainPane;

  private final OutputPane output;
  private final REPLEditor prompt;
  //private final VarsPane vars;

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

    repl = new ScriptREPL(context, "Python (Jython)", output.getOutputStream());
    repl.initialize();

    final Writer writer = output.getOutputWriter();
    final ScriptContext ctx = repl.getInterpreter().getEngine().getContext();
    ctx.setErrorWriter(writer);
    ctx.setWriter(writer);

    //vars = new VarsPane(context, repl);
    //vars.setBorder(new EmptyBorder(0, 0, 8, 0));

    //prompt = new REPLEditor(repl, vars, output);
    prompt = new REPLEditor(repl, null, output);
    context.inject(prompt);

    final JScrollPane promptScroll = new JScrollPane(prompt);

    final JPanel bottomPane = new JPanel();
    bottomPane.setLayout(new MigLayout("ins 0", "[grow,fill][pref]", "[grow,fill,align top]"));
    bottomPane.add(promptScroll, "spany 2");

    final JSplitPane outputAndPromptPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputScroll, bottomPane);
    outputAndPromptPane.setResizeWeight(1);
//    outputAndPromptPane.setDividerSize(2);

//    mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vars,
//            outputAndPromptPane);
//    mainPane.setDividerSize(1);
//    mainPane.setDividerLocation(0);
    mainPane = outputAndPromptPane;
  }

  // -- InterpreterPane methods --

  /** Gets the associated script REPL.
   *
   * @return REPL for use
   */
  public ScriptREPL getREPL() {
    return repl;
  }

  /** Prints a message to the output panel.
   *
   * @param string to show in REPL
   */
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

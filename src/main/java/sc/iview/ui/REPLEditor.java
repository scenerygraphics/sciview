/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptREPL;
import org.scijava.script.ScriptService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.OutputPane;
import org.scijava.ui.swing.script.VarsPane;
import org.scijava.util.ClassUtils;
import org.scijava.util.Types;

import javax.script.ScriptContext;
import java.awt.event.KeyEvent;

/**
 * REPL editor
 *
 * @author Ulrik Günther
 */
public class REPLEditor extends EditorPane {
  protected ScriptREPL repl;

  @Parameter
  Context context;

  @Parameter
  private ScriptService scriptService;

  @Parameter
  private LogService logService;

  protected OutputPane outputPane;

  protected VarsPane varsPane;

  protected boolean executing = false;

  public REPLEditor(ScriptREPL repl, VarsPane vars, OutputPane output) {
    super();
    this.repl = repl;
    this.outputPane = output;
    this.varsPane = vars;
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    if(executing) {
      e.consume();
      return;
    }

    if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_UP && e.getID() == KeyEvent.KEY_RELEASED) {
      walk(false);
      e.consume();
      return;
    }

    if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DOWN && e.getID() == KeyEvent.KEY_RELEASED) {
      walk(true);
      e.consume();
      return;
    }

    if(!e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_RELEASED) {
      String text = getText();
      if(text.length() == 0) {
        e.consume();
        return;
      }

      if(text.endsWith("\n")) {
        System.out.println("Truncating whitespace");
        text = text.substring(0, text.length()-1);
      }
      outputPane.append(">> " + text + "\n");
      executing = true;

      String finalText = text;
      threadService().run(() -> {
        final ScriptContext ctx = repl.getInterpreter().getEngine().getContext();
        ctx.setErrorWriter(outputPane.getOutputWriter());
        ctx.setWriter(outputPane.getOutputWriter());

        final boolean result = repl.evaluate(finalText);
        threadService().queue(() -> {
          executing = false;
          if (!result) {
            outputPane.append("REPL error occured\n");
            logService.warn("REPL error occured");
          }
          //varsPane.update();
        });
      });

      setText("");
      e.consume();
    } else {
      super.processKeyEvent(e);
      e.consume();
    }
  }

  private void walk(boolean forward) {
    setText(repl.getInterpreter().walkHistory(getText(), forward));
  }

  private ThreadService threadService() {
    // HACK: Get the SciJava context from the REPL.
    // This can be fixed if/when the REPL offers a getter for it.
    final Context ctx = (Context) ClassUtils.getValue(//
            Types.field(repl.getClass(), "context"), repl);
    return ctx.service(ThreadService.class);
  }

  void setREPLLanguage(String language) {
    System.out.println("Resetting language to " + language);
    if(!repl.getInterpreter().getLanguage().getNames().contains(language)) {
      repl.lang(language);
    }
    ScriptLanguage l = scriptService.getLanguageByName(language);
    setLanguage(scriptService.getLanguageByName(language));

    final ScriptContext ctx = repl.getInterpreter().getEngine().getContext();
    ctx.setErrorWriter(outputPane.getOutputWriter());
    ctx.setWriter(outputPane.getOutputWriter());
  }
}

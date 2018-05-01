/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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

package sc.iview.javafx;

import org.scijava.input.Accelerator;
import org.scijava.input.InputModifiers;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.ModifierValue;

/**
 * Utility class for converting between SciJava and JavaFX key constructs.
 *
 * @author Curtis Rueden
 */
@SuppressWarnings("restriction")
public final class JavaFXKeys {

    private JavaFXKeys() {
        // NB: Prevent instantiation of utility class.
    }

    /**
     * Converts a SciJava {@link Accelerator} to a JavaFX
     * {@link KeyCodeCombination}.
     */
    public static KeyCodeCombination keyCombination( final Accelerator accelerator ) {
        final InputModifiers modifiers = accelerator.getModifiers();
        final ModifierValue shift = modifierValue(modifiers.isShiftDown());
        final ModifierValue control = modifierValue(modifiers.isCtrlDown());
        final ModifierValue alt = modifierValue(modifiers.isAltDown());
        final ModifierValue meta = modifierValue(modifiers.isMetaDown());
        final ModifierValue shortcut = ModifierValue.UP; // NB: Already handled by SciJava.
        return new KeyCodeCombination( keyCode( accelerator.getKeyCode() ), shift, control, alt, meta, shortcut );
    }

    /**
     * Converts a {@link org.scijava.input.KeyCode} to a
     * {@link javafx.scene.input.KeyCode}.
     */
    public static KeyCode keyCode( org.scijava.input.KeyCode sjKeyCode ) {
        switch( sjKeyCode ) {
        case A:
            return KeyCode.A;
        case ACCEPT:
            return KeyCode.ACCEPT;
        case AGAIN:
            return KeyCode.AGAIN;
        case ALL_CANDIDATES:
            return KeyCode.ALL_CANDIDATES;
        case ALPHANUMERIC:
            return KeyCode.ALPHANUMERIC;
        case ALT:
            return KeyCode.ALT;
        case ALT_GRAPH:
            return KeyCode.ALT_GRAPH;
        case AMPERSAND:
            return KeyCode.AMPERSAND;
        case ASTERISK:
            return KeyCode.ASTERISK;
        case AT:
            return KeyCode.AT;
        case B:
            return KeyCode.B;
        case BACK_QUOTE:
            return KeyCode.BACK_QUOTE;
        case BACK_SLASH:
            return KeyCode.BACK_SLASH;
        case BACK_SPACE:
            return KeyCode.BACK_SPACE;
        case BEGIN:
            return KeyCode.BEGIN;
        case BRACELEFT:
            return KeyCode.BRACELEFT;
        case BRACERIGHT:
            return KeyCode.BRACERIGHT;
        case C:
            return KeyCode.C;
        case CANCEL:
            return KeyCode.CANCEL;
        case CAPS_LOCK:
            return KeyCode.CAPS;
        case CIRCUMFLEX:
            return KeyCode.CIRCUMFLEX;
        case CLEAR:
            return KeyCode.CLEAR;
        case CLOSE_BRACKET:
            return KeyCode.CLOSE_BRACKET;
        case CODE_INPUT:
            return KeyCode.CODE_INPUT;
        case COLON:
            return KeyCode.COLON;
        case COMMA:
            return KeyCode.COMMA;
        case COMPOSE:
            return KeyCode.COMPOSE;
        case CONTEXT_MENU:
            return KeyCode.CONTEXT_MENU;
        case CONTROL:
            return KeyCode.CONTROL;
        case CONVERT:
            return KeyCode.CONVERT;
        case COPY:
            return KeyCode.COPY;
        case CUT:
            return KeyCode.CUT;
        case D:
            return KeyCode.D;
        case DEAD_ABOVEDOT:
            return KeyCode.DEAD_ABOVEDOT;
        case DEAD_ABOVERING:
            return KeyCode.DEAD_ABOVERING;
        case DEAD_ACUTE:
            return KeyCode.DEAD_ACUTE;
        case DEAD_BREVE:
            return KeyCode.DEAD_BREVE;
        case DEAD_CARON:
            return KeyCode.DEAD_CARON;
        case DEAD_CEDILLA:
            return KeyCode.DEAD_CEDILLA;
        case DEAD_CIRCUMFLEX:
            return KeyCode.DEAD_CIRCUMFLEX;
        case DEAD_DIAERESIS:
            return KeyCode.DEAD_DIAERESIS;
        case DEAD_DOUBLEACUTE:
            return KeyCode.DEAD_DOUBLEACUTE;
        case DEAD_GRAVE:
            return KeyCode.DEAD_GRAVE;
        case DEAD_IOTA:
            return KeyCode.DEAD_IOTA;
        case DEAD_MACRON:
            return KeyCode.DEAD_MACRON;
        case DEAD_OGONEK:
            return KeyCode.DEAD_OGONEK;
        case DEAD_SEMIVOICED_SOUND:
            return KeyCode.DEAD_SEMIVOICED_SOUND;
        case DEAD_TILDE:
            return KeyCode.DEAD_TILDE;
        case DEAD_VOICED_SOUND:
            return KeyCode.DEAD_VOICED_SOUND;
        case DELETE:
            return KeyCode.DELETE;
        case DOLLAR:
            return KeyCode.DOLLAR;
        case DOWN:
            return KeyCode.DOWN;
        case E:
            return KeyCode.E;
        case END:
            return KeyCode.END;
        case ENTER:
            return KeyCode.ENTER;
        case EQUALS:
            return KeyCode.EQUALS;
        case ESCAPE:
            return KeyCode.ESCAPE;
        case EURO_SIGN:
            return KeyCode.EURO_SIGN;
        case EXCLAMATION_MARK:
            return KeyCode.EXCLAMATION_MARK;
        case F:
            return KeyCode.F;
        case F1:
            return KeyCode.F1;
        case F10:
            return KeyCode.F10;
        case F11:
            return KeyCode.F11;
        case F12:
            return KeyCode.F12;
        case F13:
            return KeyCode.F13;
        case F14:
            return KeyCode.F14;
        case F15:
            return KeyCode.F15;
        case F16:
            return KeyCode.F16;
        case F17:
            return KeyCode.F17;
        case F18:
            return KeyCode.F18;
        case F19:
            return KeyCode.F19;
        case F2:
            return KeyCode.F2;
        case F20:
            return KeyCode.F20;
        case F21:
            return KeyCode.F21;
        case F22:
            return KeyCode.F22;
        case F23:
            return KeyCode.F23;
        case F24:
            return KeyCode.F24;
        case F3:
            return KeyCode.F3;
        case F4:
            return KeyCode.F4;
        case F5:
            return KeyCode.F5;
        case F6:
            return KeyCode.F6;
        case F7:
            return KeyCode.F7;
        case F8:
            return KeyCode.F8;
        case F9:
            return KeyCode.F9;
        case FINAL:
            return KeyCode.FINAL;
        case FIND:
            return KeyCode.FIND;
        case FULL_WIDTH:
            return KeyCode.FULL_WIDTH;
        case G:
            return KeyCode.G;
        case GREATER:
            return KeyCode.GREATER;
        case H:
            return KeyCode.H;
        case HALF_WIDTH:
            return KeyCode.HALF_WIDTH;
        case HELP:
            return KeyCode.HELP;
        case HIRAGANA:
            return KeyCode.HIRAGANA;
        case HOME:
            return KeyCode.HOME;
        case I:
            return KeyCode.I;
        case INPUT_METHOD_ON_OFF:
            return KeyCode.INPUT_METHOD_ON_OFF;
        case INSERT:
            return KeyCode.INSERT;
        case INVERTED_EXCLAMATION_MARK:
            return KeyCode.INVERTED_EXCLAMATION_MARK;
        case J:
            return KeyCode.J;
        case JAPANESE_HIRAGANA:
            return KeyCode.JAPANESE_HIRAGANA;
        case JAPANESE_KATAKANA:
            return KeyCode.JAPANESE_KATAKANA;
        case JAPANESE_ROMAN:
            return KeyCode.JAPANESE_ROMAN;
        case K:
            return KeyCode.K;
        case KANA:
            return KeyCode.KANA;
        case KANA_LOCK:
            return KeyCode.KANA_LOCK;
        case KANJI:
            return KeyCode.KANJI;
        case KATAKANA:
            return KeyCode.KATAKANA;
        case KP_DOWN:
            return KeyCode.KP_DOWN;
        case KP_LEFT:
            return KeyCode.KP_LEFT;
        case KP_RIGHT:
            return KeyCode.KP_RIGHT;
        case KP_UP:
            return KeyCode.KP_UP;
        case L:
            return KeyCode.L;
        case LEFT:
            return KeyCode.LEFT;
        case LEFT_PARENTHESIS:
            return KeyCode.LEFT_PARENTHESIS;
        case LESS:
            return KeyCode.LESS;
        case M:
            return KeyCode.M;
        case META:
            return KeyCode.META;
        case MINUS:
            return KeyCode.MINUS;
        case MODECHANGE:
            return KeyCode.MODECHANGE;
        case N:
            return KeyCode.N;
        case NONCONVERT:
            return KeyCode.NONCONVERT;
        case NUM0:
            return KeyCode.DIGIT0;
        case NUM1:
            return KeyCode.DIGIT1;
        case NUM2:
            return KeyCode.DIGIT2;
        case NUM3:
            return KeyCode.DIGIT3;
        case NUM4:
            return KeyCode.DIGIT4;
        case NUM5:
            return KeyCode.DIGIT5;
        case NUM6:
            return KeyCode.DIGIT6;
        case NUM7:
            return KeyCode.DIGIT7;
        case NUM8:
            return KeyCode.DIGIT8;
        case NUM9:
            return KeyCode.DIGIT9;
        case NUMBER_SIGN:
            return KeyCode.NUMBER_SIGN;
        case NUMPAD_0:
            return KeyCode.NUMPAD0;
        case NUMPAD_1:
            return KeyCode.NUMPAD1;
        case NUMPAD_2:
            return KeyCode.NUMPAD2;
        case NUMPAD_3:
            return KeyCode.NUMPAD3;
        case NUMPAD_4:
            return KeyCode.NUMPAD4;
        case NUMPAD_5:
            return KeyCode.NUMPAD5;
        case NUMPAD_6:
            return KeyCode.NUMPAD6;
        case NUMPAD_7:
            return KeyCode.NUMPAD7;
        case NUMPAD_8:
            return KeyCode.NUMPAD8;
        case NUMPAD_9:
            return KeyCode.NUMPAD9;
        case NUMPAD_ASTERISK:
            return KeyCode.MULTIPLY;
        case NUMPAD_MINUS:
            return KeyCode.SUBTRACT;
        case NUMPAD_PERIOD:
            return KeyCode.DECIMAL;
        case NUMPAD_PLUS:
            return KeyCode.ADD;
        case NUMPAD_SEPARATOR:
            return KeyCode.SEPARATOR;
        case NUMPAD_SLASH:
            return KeyCode.DIVIDE;
        case NUM_LOCK:
            return KeyCode.NUM_LOCK;
        case O:
            return KeyCode.O;
        case OPEN_BRACKET:
            return KeyCode.OPEN_BRACKET;
        case P:
            return KeyCode.P;
        case PAGE_DOWN:
            return KeyCode.PAGE_DOWN;
        case PAGE_UP:
            return KeyCode.PAGE_UP;
        case PASTE:
            return KeyCode.PASTE;
        case PAUSE:
            return KeyCode.PAUSE;
        case PERIOD:
            return KeyCode.PERIOD;
        case PLUS:
            return KeyCode.PLUS;
        case PREVIOUS_CANDIDATE:
            return KeyCode.PREVIOUS_CANDIDATE;
        case PRINTSCREEN:
            return KeyCode.PRINTSCREEN;
        case PROPS:
            return KeyCode.PROPS;
        case Q:
            return KeyCode.Q;
        case QUOTE:
            return KeyCode.QUOTE;
        case QUOTEDBL:
            return KeyCode.QUOTEDBL;
        case R:
            return KeyCode.R;
        case RIGHT:
            return KeyCode.RIGHT;
        case RIGHT_PARENTHESIS:
            return KeyCode.RIGHT_PARENTHESIS;
        case ROMAN_CHARACTERS:
            return KeyCode.ROMAN_CHARACTERS;
        case S:
            return KeyCode.S;
        case SCROLL_LOCK:
            return KeyCode.SCROLL_LOCK;
        case SEMICOLON:
            return KeyCode.SEMICOLON;
        case SHIFT:
            return KeyCode.SHIFT;
        case SLASH:
            return KeyCode.SLASH;
        case SPACE:
            return KeyCode.SPACE;
        case STOP:
            return KeyCode.STOP;
        case T:
            return KeyCode.T;
        case TAB:
            return KeyCode.TAB;
        case U:
            return KeyCode.U;
        case UNDEFINED:
            return KeyCode.UNDEFINED;
        case UNDERSCORE:
            return KeyCode.UNDERSCORE;
        case UNDO:
            return KeyCode.UNDO;
        case UP:
            return KeyCode.UP;
        case V:
            return KeyCode.V;
        case W:
            return KeyCode.W;
        case WINDOWS:
            return KeyCode.WINDOWS;
        case X:
            return KeyCode.X;
        case Y:
            return KeyCode.Y;
        case Z:
            return KeyCode.Z;
        default:
            return null;
        }
    }

    private static ModifierValue modifierValue(final boolean down) {
        return down ? ModifierValue.DOWN : ModifierValue.UP;
    }
}

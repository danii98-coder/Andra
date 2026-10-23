package pe.edu.unaj.andara.util;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public final class Portapapeles {

    private Portapapeles() {
    }

    public static void copiar(String texto) {
        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(texto);
        Clipboard.getSystemClipboard().setContent(contenido);
    }
}

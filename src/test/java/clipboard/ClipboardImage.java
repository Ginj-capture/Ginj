package clipboard;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * From https://coderanch.com/t/333565/java/BufferedImage-System-Clipboard
 */
public class ClipboardImage implements ClipboardOwner {
    public ClipboardImage() {
        try {
            for (int i = 0; i < 100; i++) {
                Robot robot = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle screen = new Rectangle(screenSize);
                BufferedImage bufferedImage = robot.createScreenCapture(screen);
                TransferableImage trans = new TransferableImage(bufferedImage);
                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                c.setContents(trans, this);
                System.out.println("Image " + i + " copied.");
            }
        }
        catch ( AWTException x ) {
            x.printStackTrace();
            System.exit( 1 );
        }
    }

    public static void main( String[] arg ) {
        ClipboardImage ci = new ClipboardImage();
    }

    public void lostOwnership( Clipboard clip, Transferable trans ) {
        System.out.println( "Lost Clipboard Ownership" );
    }

    private class TransferableImage implements Transferable {

        Image i;

        public TransferableImage( Image i ) {
            this.i = i;
        }

        public Object getTransferData( DataFlavor flavor )
                throws UnsupportedFlavorException, IOException {
            if ( flavor.equals( DataFlavor.imageFlavor ) && i != null ) {
                return i;
            }
            else {
                throw new UnsupportedFlavorException( flavor );
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] flavors = new DataFlavor[ 1 ];
            flavors[ 0 ] = DataFlavor.imageFlavor;
            return flavors;
        }

        public boolean isDataFlavorSupported( DataFlavor flavor ) {
            DataFlavor[] flavors = getTransferDataFlavors();
            for ( int i = 0; i < flavors.length; i++ ) {
                if ( flavor.equals( flavors[ i ] ) ) {
                    return true;
                }
            }

            return false;
        }
    }
}
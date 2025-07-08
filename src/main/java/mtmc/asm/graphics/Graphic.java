package mtmc.asm.graphics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import mtmc.asm.ASMElement;
import mtmc.emulator.MTMCDisplay;
import mtmc.tokenizer.MTMCToken;

/**
 *
 * @author jbanes
 */
public class Graphic extends ASMElement {
    
    private String filename;
    private byte[] data;

    public Graphic(List<MTMCToken> labels, int lineNumber) {
        super(labels, lineNumber);
    }
    
    public void setImage(String filename) {
        try {
            var image = ImageIO.read(new File(filename));
            var buffer = new ByteArrayOutputStream();
            
            if (image.getWidth() > 1024 || image.getHeight() > 1024) {
                addError(filename + " is too large. Maximum image size is 1024x1024");
            }
            
            image = MTMCDisplay.convertImage(image);
            
            ImageIO.write(image, "png", buffer);
            
            this.filename = filename;
            this.data = buffer.toByteArray();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            addError(filename + " not found"); 
        } catch(IOException e) {
            e.printStackTrace();
            addError(e.getMessage()); // TODO: Verify these messages are meaningful
        }
    }
    
    @Override
    public void addError(String err) {
        addError(getLabels().getLast(), err);
    }
    
    @Override
    public int getSizeInBytes() {
        return 2;
    }
    
    public byte[] getImageData() {
        return data;
    }
    
}

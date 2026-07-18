package mime;


/**
 * @author charlottexiao
 */
public class Ncm {

    private String ncmFile;
    private String outFile;
    private Mata mata;
    private byte[] image;

    public Ncm() {
    }

    public String getNcmFile() {
        return ncmFile;
    }

    public void setNcmFile(String ncmFile) {
        this.ncmFile = ncmFile;
    }

    public String getOutFile() {
        return outFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }

    public Mata getMata() {
        return mata;
    }

    public void setMata(Mata mata) {
        this.mata = mata;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
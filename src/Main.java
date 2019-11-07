import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        //讲pdf转成图片
        //pdf2Image("/Users/zhangminglei/Temp/test.pdf","/Users/zhangminglei/Temp/",200);
        //显示裁切的框
        //showTextRegion("/Users/zhangminglei/Temp/","test_0.png");
        //裁切
        clipSaveText("/Users/zhangminglei/Temp/","test_0.png");
    }

    public static void clearNoiseContours(List<MatOfPoint> contours,int area) {
        Iterator<MatOfPoint> iterator = contours.iterator();
        while (iterator.hasNext()) {
            MatOfPoint matOfPoint = iterator.next();
            MatOfPoint2f mat2f=new MatOfPoint2f();
            matOfPoint.convertTo(mat2f,CvType.CV_32FC1);
            RotatedRect rect=Imgproc.minAreaRect(mat2f);
            if (rect.boundingRect().area() < area) {
                iterator.remove();
            };
        }
    }

    public static void showImg(String path) {
        Mat image = Imgcodecs.imread(path);
        showImg(image);
    }

    public static void showImg(Mat mat) {
        ImageViewer imageViewer = new ImageViewer(mat,"hello");
        imageViewer.imshow();
    }

    public static void clipSaveText(String path,String filename) {
        Mat image = Imgcodecs.imread(path+filename);
        Mat visualImage = image.clone();
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        //二值
        double[] data = image.get(0, 0);
        int thres = (int)(data[0] - 5);
        Imgproc.threshold(image, image, thres, 255, Imgproc.THRESH_BINARY);
        //反色
        Core.bitwise_not(image, image);
        image = dilate(image,30);
        //showImg(image);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println(contours.size());
        clearNoiseContours(contours,50000);
        for (int i = 0; i < contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));
            Mat mat = visualImage.submat(rect);
            Imgcodecs.imwrite(path+filename+"_clip"+i+".png", mat);
        }
    }


    //For test
    public static void showTextRegion(String path,String filename) {
        Mat image = Imgcodecs.imread(path+filename);
        Mat visualImage = image.clone();
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        //二值
        double[] data = image.get(0, 0);
        int thres = (int)(data[0] - 5);
        Imgproc.threshold(image, image, thres, 255, Imgproc.THRESH_BINARY);
        //反色
        Core.bitwise_not(image, image);
        image = dilate(image,30);
        //showImg(image);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println(contours.size());
        clearNoiseContours(contours,50000);
        for (int i = 0; i < contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));
            Imgproc.rectangle(visualImage,rect.tl(),rect.br(),new Scalar(0,0,255),3);
        }
        Mat testImage = new Mat(visualImage,Imgproc.boundingRect(contours.get(1)));
        showImg(visualImage);
    }

    /**
     * 模糊处理
     * @param mat
     * @return
     */
    public static Mat blur (Mat mat) {
        Mat blur = new Mat();
        Imgproc.blur(mat,blur,new Size(5,5));
        return blur;
    }

    /**
     *膨胀
     * @param mat
     * @return
     */
    public static Mat dilate (Mat mat,int size){
        Mat dilate=new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(size,size));
        //膨胀
        Imgproc.dilate(mat, dilate, element, new Point(-1, -1), 1);
        return dilate;
    }

    /**
     * 腐蚀
     * @param mat
     * @return
     */
    public static Mat erode (Mat mat,int size){
        Mat erode=new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(size,size));
        //腐蚀
        Imgproc.erode(mat, erode, element, new Point(-1, -1), 1);
        return erode;
    }




    public static String pdf2Image(String PdfFilePath, String dstImgFolder, int dpi) {
        File file = new File(PdfFilePath);
        PDDocument pdDocument;
        try {
            String imgPDFPath = file.getParent();
            int dot = file.getName().lastIndexOf('.');
            String imagePDFName = file.getName().substring(0, dot); // 获取图片文件名
            String imgFolderPath = dstImgFolder;

            pdDocument = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(pdDocument);
            /* dpi越大转换后越清晰，相对转换速度越慢 */
            StringBuffer imgFilePath = null;
            for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
                System.out.println("正在转换第"+i+"页");
                String imgFilePathPrefix = imgFolderPath + File.separator + imagePDFName;
                imgFilePath = new StringBuffer();
                imgFilePath.append(imgFilePathPrefix);
                imgFilePath.append("_");
                imgFilePath.append(+i);
                imgFilePath.append(".png");
                File dstFile = new File(imgFilePath.toString());
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                ImageIO.write(image, "png", dstFile);
                System.out.println("第"+i+"页转换完成");
            }
            System.out.println("PDF文档转PNG图片成功！");
            return imgFilePath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ExifEditor {

    public static void modifyExifTime(File imageFile, LocalDateTime baseTime, LocalDateTime targetTime, String operation) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(imageFile);
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                TiffImageMetadata exifMetadata = jpegMetadata.getExif();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

                Duration duration = null;

                if (exifMetadata != null) {
                    String dateTaken = exifMetadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getValueDescription().substring(1,20);
                    //获取原始EXIF时间
                    LocalDateTime originalTime = LocalDateTime.parse(dateTaken, formatter);

                    duration = Duration.between(baseTime, originalTime);
                    LocalDateTime resultTime = targetTime.plus(duration);

                    if (operation.equalsIgnoreCase("modify")) {

                        TiffOutputSet outputSet = exifMetadata != null ? exifMetadata.getOutputSet() : new TiffOutputSet();

                        //移除原有EXIF时间
                        TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);

                        //写入新EXIF时间
                        String resultTimeString = resultTime.format(formatter);
                        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, resultTimeString);
                        File tempFile = File.createTempFile("tempImage", ".jpg");
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            new ExifRewriter().updateExifMetadataLossless(imageFile, fos, outputSet);
                        }

                        // 替换原始文件
                        if (imageFile.delete() && tempFile.renameTo(imageFile)) {
                            System.out.println("拍摄日期已修改成功：" + resultTimeString);
                        } else {
                            System.out.println("无法替换原始文件。");
                        }
                    }else {
                        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String resultTimeString = resultTime.format(formatter);
                        System.out.println("文件名：" + imageFile.getName() + "拟替换的结果时间：" + resultTimeString);
                    }

                }
            }


        } catch (Exception e) {
            //很多图片可能读取exif出现异常为正常现象 通常无需处理
            e.printStackTrace();
        }

    }

    public static void main(String args[]){
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("请输入基准时间B（格式：yyyy-MM-dd HH:mm:ss）：");
        String baseTimeInput = scanner.nextLine();
        LocalDateTime targetTime = LocalDateTime.parse(baseTimeInput, formatter);

        System.out.println("请输入原始时间T（格式：yyyy-MM-dd HH:mm:ss）：");
        String secondTimeInput = scanner.nextLine();
        LocalDateTime baseTime = LocalDateTime.parse(secondTimeInput, formatter);

        System.out.println("请输入本次操作类型，修改请输入modify");
        String operation = scanner.nextLine();

        // 指定要列出的目录
        String directoryPath = "D:\\Pictures\\生活\\2024.6.8 写真集\\6";

        // 创建文件对象
        File directory = new File(directoryPath);

        // 确认目录存在且是一个目录
        if (directory.exists() && directory.isDirectory()) {
            // 获取目录下的所有文件和子目录
            File[] files = directory.listFiles();

            if (files != null) {
                // 列出所有文件和子目录
                for (File file : files) {
                    modifyExifTime(file, baseTime, targetTime, operation);
                }
            } else {
                System.out.println("该目录是空的。");
            }
        } else {
            System.out.println("目录不存在或不是一个目录。");
        }
    }

}

package com.ae.pushstream;

import cn.hutool.core.io.resource.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ffmpeg推流到SRS
 * @author AldrichEugene
 * @since 2018-07-15
 */
@Component
public class PushCameraStreamByFFmpeg implements ApplicationRunner {

    @Value("${ae.url.sourceAddress}")
    private String sourceAddress;

    @Value("${ae.url.targetAddress}")
    private String targetAddress;

    @Value("${ae.resources.ffmpegPath}")
    private String ffmpegPath;

    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            2,
            5,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy());

    /**
     * springboot容器启动之后执行
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) {
        threadPool.execute(() -> {
            startPushCameraStream(sourceAddress,targetAddress);
        });
    }

    /**
     * 推流
     * 完整地址 ffmpeg -re -i rtsp://admin:zd199611@192.168.0.109:554/h264/1/main/av_stream -vcodec copy -acodec copy -f flv -y rtmp://192.168.145.201:1935/live/livestream
     * 播放地址 http://192.168.145.201:8080/live/livestream.flv
     * @param sourceAddress 摄像头地址 rtsp://admin:zd199611@192.168.0.109:554/h264/1/main/av_stream
     * @param targetAddress SRS地址   rtmp://192.168.145.201:1935/live/livestream
     */
    public void startPushCameraStream(String sourceAddress, String targetAddress){
        try {
            String path = getFFmpegPath();
            String command = path + "ffmpeg -re -i " + sourceAddress + " -vcodec copy -acodec copy -f flv -y " + targetAddress;
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while((line = br.readLine()) != null) {
                System.out.println("视频推流信息[" + line + "]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取ffmpeg.exe的路径
     * @return
     */
    private String getFFmpegPath(){
        String os = null;
        String osName = System.getProperties().getProperty("os.name");
        System.out.println("current system :"+osName);
        if(osName.contains("Windows")){
            os = "win";
        }else if(osName.contains("Linux")){
            os = "linux";
        }else {
            throw new RuntimeException("This operating system is not supported!");
        }
        if(os == null){
            throw new RuntimeException("ffmpeg.exe path not found!");
        }

        ClassPathResource classPathResource = new ClassPathResource(ffmpegPath + File.separator + os + File.separator);
        String path = classPathResource.getAbsolutePath();
        return path;
    }
}

package com.iflytek.sdk.client.transfer;


import com.google.gson.Gson;
import com.iflytek.sdk.dto.RequestFile;
import com.iflytek.sdk.dto.ResponseFile;
import com.iflytek.sdk.dto.SecureModel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author： liwh
 * @Date: 2016/11/17.
 * @Description：
 */
public class FileTransferClientHandler extends ChannelInboundHandlerAdapter {

    private int byteRead;
    private volatile long start = 0;
    public RandomAccessFile randomAccessFile;
    private RequestFile request;
    private final int minReadBufferSize = 10192;


    /**
     *
     * @param ef
     */
    public FileTransferClientHandler(RequestFile ef){
        if(ef.getFile().exists()){ //==============>>>>>>>>>> 存在文件
            if(!ef.getFile().isFile()){ //=========>>>>>>>>>> 是否是文件
                System.out.println("Not a file :" + ef.getFile());
                return;
            }

            this.request =ef;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {


        SecureModel secure = new SecureModel();
        secure.setToken("2222222222222");
        ctx.writeAndFlush(secure);

//        String str = "文件已经读完";
//        sendMessage(str);
//        ctx.writeAndFlush(request);
    }

    public  RequestFile sendMessage(String str) {
        try {
            randomAccessFile = new RandomAccessFile(request.getFile(), "r");
            randomAccessFile.seek(request.getStarPos());
            byte[] bytes = new byte[minReadBufferSize];
            if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                request.setEndPos(byteRead);
                request.setBytes(bytes);
                request.setFile_size(randomAccessFile.length());
                return request;
            } else {
                System.out.println(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
        return null;
    }

    /**
     * channelRead 读取
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Gson gson = new Gson();
        System.out.println(gson.toJson(msg));
        if(msg instanceof SecureModel){
            try {
                randomAccessFile =new RandomAccessFile(request.getFile(),"r");
                randomAccessFile.seek(request.getStarPos());
                byte[] bytes = new byte[minReadBufferSize];
                if((byteRead =randomAccessFile.read(bytes)) !=-1){//  =====>>>>>>读写文件
                    request.setEndPos(byteRead);
                    request.setBytes(bytes);
                    request.setFile_size(randomAccessFile.length());
                    ctx.writeAndFlush(request);
                }else{
                    System.out.println("The file has been read！");// =============================>>>>>>>文件读取完毕！
                }
            }catch (FileNotFoundException e){ //=======>>>文件未找着
                e.printStackTrace();
            }catch (IOException io){
                io.printStackTrace();
            }
            return;
        }

        if(msg instanceof ResponseFile){
            ResponseFile response = (ResponseFile)msg;
            if(response.isFileEnd()){// ================>>>>>>>>> 响应是否结束
                randomAccessFile.close();
                ctx.close();
            }else{
                start = response.getStart();
                if (start != -1) {
                    randomAccessFile = new RandomAccessFile(request.getFile(), "r");
                    randomAccessFile.seek(start);
                    int a = (int) (randomAccessFile.length() - start);
                    int sendLength = minReadBufferSize;
                    if (a < minReadBufferSize) {
                        sendLength = a;
                    }
                    byte[] bytes = new byte[sendLength];
                    if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - start) > 0) {
                        request.setEndPos(byteRead);
                        request.setBytes(bytes);
                        try {
                            ctx.writeAndFlush(request);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        randomAccessFile.close();
                        ctx.close();
                    }
                }
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    //================>>>>>>> getter and setter >>>>>>================

    public int getByteRead() {
        return byteRead;
    }

    public void setByteRead(int byteRead) {
        this.byteRead = byteRead;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    public void setRandomAccessFile(RandomAccessFile randomAccessFile) {
        this.randomAccessFile = randomAccessFile;
    }

    public RequestFile getRequest() {
        return request;
    }

    public void setRequest(RequestFile request) {
        this.request = request;
    }

    public int getMinReadBufferSize() {
        return minReadBufferSize;
    }
}

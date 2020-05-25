package com.olcom.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * .
 *
 * @ClassName: SocketClient
 * @Auther: olcom
 * @Date: 2019/8/29 11:29:28
 * @Version: iom-cloud-platformV1.0
 * @Description: socket 客户端,简单封装.
 **/
@Slf4j
public class SocketClient {

  private String ip;
  private int port;
  private Socket socket;
  private DataOutputStream dataOutputStream;
  private DataInputStream dataInputStream;
  private Date useDate; //连接使用时间,用于判断session失效

  public SocketClient(String ip, int port) {
    this.ip = ip;
    this.port = port;
    useDate = new Date();
  }

  public void connect() throws Exception {
    log.debug("Entry Method:connect()");
    try {
      close();  //1.主动释放连接 //2.某些服务器对指定ip有链路数限制

      socket = new Socket();
      //socket.setKeepAlive(true);
      SocketAddress socketAddress = new InetSocketAddress(ip, port);
      socket.connect(socketAddress, 1000); //某些服务器ping延迟高时要增加,否则会报错connect timeout

      dataOutputStream = new DataOutputStream(socket.getOutputStream());
      dataInputStream = new DataInputStream(socket.getInputStream());

      updateUseDate();
    } catch (Exception e) {
      socket = null;
      log.error("socket connect error ip:" + ip + ",port:" + port + ",Exception:" + e.getMessage());

      throw new Exception(
          "socket connect error ip:" + ip + ",port:" + port + ",Exception:" + e.getMessage());
    }
    log.debug("Exit Method:connect()");
  }

  public void write(byte[] msg, int len) throws IOException {
    log.trace("dataOutputStream.write");
    dataOutputStream.write(msg, 0, len);
    log.trace("dataOutputStream.flush");
    dataOutputStream.flush();
    updateUseDate();
  }

  public byte[] read(int bufferSize, int timeOut) throws IOException {
    socket.setSoTimeout(timeOut * 1000);
    byte[] bytes = new byte[bufferSize];
    log.trace("dataInputStream.read");
    int len = dataInputStream.read(bytes);
    updateUseDate();
    log.debug("readLen:" + len);
    byte[] tempBytes = null;
    if (len > 0) {
      tempBytes = new byte[len];
      System.arraycopy(bytes, 0, tempBytes, 0, len);
    }
    return tempBytes;
  }

  public void close() {
    log.debug("Entry Method:close()");
    try {
      if (null != dataOutputStream) {
        dataOutputStream.close();
      }
      if (null != dataInputStream) {
        dataInputStream.close();
      }
      if (null != socket && !socket.isClosed()) {
        socket.close();
      }
      socket = null;
    } catch (IOException e) {
      log.error("SocketClient close Exception:" + e.getMessage());
    }
    log.debug("Exit Method:close()");
  }

  public boolean valid() throws Exception {
    if (null == socket || socket.isClosed() ||
        socket.isInputShutdown() || socket.isOutputShutdown()) {
      if (dataInputStream != null) {
        dataInputStream.close();
      }
      if (dataOutputStream != null) {
        dataOutputStream.close();
      }
      if (socket != null) {
        socket.close();
      }
      return false;
    }
    return true;
  }

  public long getTimePass() {
    log.trace("Entry Method:getTimePass(),useDate:{}", useDate.getTime());
    Date date = new Date();
    log.debug("Exit Method:getTimePass(),timePass:{}", date.getTime() - useDate.getTime());
    return (date.getTime() - useDate.getTime());
  }

  public void updateUseDate() {
    useDate = new Date();
  }

  public static void main(String[] args) {

    System.out.println("SocketClient main start");
    try {
      System.out.println("----------try start----------");
      SocketClient socketClient = new SocketClient("localhost", 8080);
      socketClient.connect();
      String strInput = "hello server !";
      socketClient.write(strInput.getBytes(), strInput.length());
      byte[] recv = socketClient.read(1024, 10);
      String strOriginal = null;
      if (null != recv) {
        strOriginal = new String(recv, StandardCharsets.ISO_8859_1);
      }
      log.info("strOriginal:" + strOriginal);

      System.out.println("----------try end----------");
    } catch (Exception e) {
      System.out.println("catch error:" + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("SocketClient main end");
  }

}

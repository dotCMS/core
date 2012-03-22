package com.dotmarketing.servlets;

import com.missiondata.fileupload.OutputStreamListener;

public class AjaxFileUploadListener implements OutputStreamListener
{
  private FileUploadStats fileUploadStats = new FileUploadStats();

  public AjaxFileUploadListener(long totalSize)
  {
    fileUploadStats.setTotalSize(totalSize);
  }

  public void start()
  {
    fileUploadStats.setCurrentStatus("start");
  }

  public void bytesRead(int byteCount)
  {
    fileUploadStats.incrementBytesRead(byteCount);
    fileUploadStats.setCurrentStatus("reading");
  }

  public void error(String s)
  {
    fileUploadStats.setCurrentStatus("error");
  }

  public void done()
  {
    fileUploadStats.setBytesRead(fileUploadStats.getTotalSize());
    fileUploadStats.setCurrentStatus("done");
  }

  public FileUploadStats getFileUploadStats()
  {
    return fileUploadStats;
  }

  public static class FileUploadStats
  {
    private long totalSize = 0;
    private long bytesRead = 0;
    private long startTime = System.currentTimeMillis();
    private String currentStatus = "none";

    public long getTotalSize()
    {
      return totalSize;
    }

    public void setTotalSize(long totalSize)
    {
      this.totalSize = totalSize;
    }

    public long getBytesRead()
    {
      return bytesRead;
    }

    public long getElapsedTimeInSeconds()
    {
      return (System.currentTimeMillis() - startTime) / 1000;
    }

    public String getCurrentStatus()
    {
      return currentStatus;
    }

    public void setCurrentStatus(String currentStatus)
    {
      this.currentStatus = currentStatus;
    }

    public void setBytesRead(long bytesRead)
    {
      this.bytesRead = bytesRead;
    }

    public void incrementBytesRead(int byteCount)
    {
      this.bytesRead += byteCount;
    }
  }
}

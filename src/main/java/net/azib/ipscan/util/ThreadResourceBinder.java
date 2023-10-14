package net.azib.ipscan.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.Closeable;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadResourceBinder<T> {

  private final Map<Long, T> resources = new ConcurrentHashMap<>(256);

  public T bind(T resource) {
    resources.put(Thread.currentThread().getId(), resource);
    return resource;
  }

  public void close() {
    for (T resource : resources.values()) close(resource);
    resources.clear();
  }

  private void close(T resource) {
    if (resource instanceof DatagramSocket) closeQuietly((DatagramSocket) resource);
    else if (resource instanceof Socket) closeQuietly((Socket) resource);
    else if (resource instanceof Closeable) closeQuietly((Closeable) resource);
  }

  public void closeAndUnbind(T resource) {
    close(resource);
    resources.remove(Thread.currentThread().getId());
  }
}

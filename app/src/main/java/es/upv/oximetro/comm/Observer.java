package es.upv.oximetro.comm;


import es.upv.fastble.data.BleDevice;

public interface Observer {

    void disConnected(BleDevice bleDevice);
}

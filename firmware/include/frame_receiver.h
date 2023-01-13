#ifndef __NETWORK_H__
#define __NETWORK_H__
#include <WiFi.h>

class FrameReceiver
{
public:
    FrameReceiver(WiFiServer *server);
    ~FrameReceiver();
    bool connected();
    int recvData();
    bool hasFrame();
    // frame size is large than buf size
    // skip this frame
    bool isSkippedFrame();
    uint32_t frameSize();
    u_char *frameData();
    void nextFrame();
    void sendAck();

private:
    WiFiServer *server;
    WiFiClient client;
    uint8_t *buf;
    uint32_t maxDataSize;
    uint32_t curDataPos;
    uint32_t frameEndPos;
    bool skip;
    bool hasHead;
    uint32_t skipSize;
    void reset();
};

#endif
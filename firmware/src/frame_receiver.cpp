#include "frame_receiver.h"

FrameReceiver::FrameReceiver(WiFiServer *server)
{
    this->server = server;
    if (this->server->hasClient())
        client = this->server->available();

    // JPEG typically achieves 10:1 compression with little perceptible loss in image quality
    // width * height * sizeof(RGB565)
    // double redundant data
    maxDataSize = 240 * 240 * 2 / 10 * 2;
    // buf = [ head + data ]
    buf = (uint8_t *)malloc(maxDataSize);

    reset();
}

FrameReceiver::~FrameReceiver()
{
    client.stop();
    free(buf);
}

bool FrameReceiver::connected()
{
    return client.connected();
}

int FrameReceiver::recvData()
{
    int bytes = 0;
    int remain = 0;

    remain = maxDataSize - curDataPos;
    bytes = client.read(buf + curDataPos, remain);
    if (bytes > 0)
        curDataPos += bytes;

    return bytes;
}

bool FrameReceiver::hasFrame()
{
    if (!hasHead)
    {
        if (0xFF == buf[0] && 0xD8 == buf[1])
            hasHead = true;
    }

    if (hasHead)
    {
        for (int i = 0; i < curDataPos - 1; i++)
        {
            if (0xFF == buf[i] && 0xD9 == buf[i + 1])
            {
                frameEndPos = i + 1;
                return true;
            }
        }
        // frame size is large than buf size
        if (curDataPos == maxDataSize)
        {
            skip = true;
            skipSize += maxDataSize;
            curDataPos = 0;
        }
    }

    return false;
}

bool FrameReceiver::isSkippedFrame()
{
    return skip;
}

uint32_t FrameReceiver::frameSize()
{
    return frameEndPos + 1 + skipSize;
}

u_char *FrameReceiver::frameData()
{
    if (isSkippedFrame())
        return NULL;
    else
        return buf;
}

void FrameReceiver::nextFrame()
{
    memmove(buf + frameEndPos + 1, buf, curDataPos - frameEndPos);
    reset();
}

void FrameReceiver::reset()
{
    curDataPos = 0;
    frameEndPos = 0;
    skip = false;
    hasHead = false;
    skipSize = 0;
}

void FrameReceiver::sendAck()
{
    client.write("ACK", 3);
}
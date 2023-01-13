#include <Arduino.h>
#include "tft_setup.h"
#include <TFT_eSPI.h>
#include <Esp.h>
#include <TJpg_Decoder.h>
#include <Preferences.h>
#include "frame_receiver.h"

static TFT_eSPI tft;
static WiFiServer server;

static String wifiName;
static String wifiPasswd;

static FrameReceiver *frameReciver = NULL;
static unsigned long recvTime = 0;

void echoSystemInfo()
{
  Serial.printf("chip revision: %d\n", ESP.getChipRevision());
  Serial.printf("chip model: %s\n", ESP.getChipModel());
  Serial.printf("chip cores: %d\n", ESP.getChipCores());
  Serial.printf("cpu FreqMHz: %d\n", ESP.getCpuFreqMHz());
  Serial.printf("sdk version: %s\n", ESP.getSdkVersion());

  Serial.printf("heap size: %d\n", ESP.getHeapSize());
  Serial.printf("free heap: %d\n", ESP.getFreeHeap());
  Serial.printf("mini free heap: %d\n", ESP.getMinFreeHeap());
  Serial.printf("max alloc heap: %d\n", ESP.getMaxAllocHeap());

  Serial.printf("psram size: %d\n", ESP.getPsramSize());
  Serial.printf("free psram: %d\n", ESP.getFreePsram());
  Serial.printf("mini free psram: %d\n", ESP.getMinFreePsram());
  Serial.printf("max alloc psram: %d\n", ESP.getMaxAllocPsram());
}

bool needConfigWiFi()
{
  return !digitalRead(KEY_BUILTIN);
}

void configWiFi()
{
  Preferences prefs;
  int r;
  prefs.begin("SimpleAR");

  wifiName = prefs.getString("WiFiName");
  wifiPasswd = prefs.getString("WiFiPasswd");
  if (needConfigWiFi() || wifiName.isEmpty() || wifiPasswd.isEmpty())
  {
    randomSeed(millis());

    wifiName = "AR-";
    // A-Z 65-90, a-z 97-122
    r = random(65, 90 - 4);
    for (int i = 0; i < 4; i++)
      wifiName += (char)(r + i);

    wifiPasswd = "";
    for (int i = 0; i < 4; i++)
    {
      r = random(97, 122);
      wifiPasswd += (char)(r);
      wifiPasswd += (char)(r - (97 - 65));
    }

    prefs.putString("WiFiName", wifiName);
    prefs.putString("WiFiPasswd", wifiPasswd);
  }

  prefs.end();
}

void displayWiFiProgress(const char *name, const char *passwd, int retryCount)
{
  int fontSize = 2;
  int lineHeight = fontSize * 8;
  int startX = tft.width() / 2;
  int startY = tft.height() / 2 - lineHeight;
  String t;

  tft.fillScreen(tft.color565(25, 25, 25));
  tft.setTextDatum(MC_DATUM);
  tft.setTextColor(TFT_WHITE, tft.color565(25, 25, 25));
  t = "WiFiName: ";
  t.concat(name);
  tft.drawString(t, startX, startY, fontSize);
  t = "WiFiPasswd: ";
  t.concat(passwd);
  tft.drawString(t, startX, startY + lineHeight, 2);
  t = "Retrying: ";
  t.concat(retryCount);
  tft.drawString(t, startX, startY + lineHeight * 2, 2);
}

void displayWiFiIP(const char *name, const char *passwd, const char *gateway, const char *ip)
{
  int fontSize = 2;
  int lineHeight = fontSize * 8;
  int startX = tft.width() / 2;
  int startY = tft.height() / 2 - lineHeight;
  String t;

  tft.fillScreen(tft.color565(25, 25, 25));
  tft.setTextDatum(MC_DATUM);
  tft.setTextColor(TFT_WHITE, tft.color565(25, 25, 25));
  t = "WiFiName: ";
  t.concat(name);
  tft.drawString(t, startX, startY, fontSize);
  t = "WiFiPasswd: ";
  t.concat(passwd);
  tft.drawString(t, startX, startY + lineHeight, 2);
  t = "Gateway: ";
  t.concat(gateway);
  tft.drawString(t, startX, startY + lineHeight * 2, 2);
  t = "IP: ";
  t.concat(ip);
  tft.drawString(t, startX, startY + lineHeight * 3, 2);
}

void connectWiFi()
{
  int retryCount = 0;
  WiFi.mode(WIFI_STA);
  WiFi.begin(wifiName.c_str(), wifiPasswd.c_str());

  Serial.println("Waiting for WiFi");
  Serial.println("name: " + wifiName);
  Serial.println("passwd: " + wifiPasswd);
  displayWiFiProgress(wifiName.c_str(), wifiPasswd.c_str(), retryCount);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(2000);
    Serial.print(".");
    retryCount++;
    displayWiFiProgress(wifiName.c_str(), wifiPasswd.c_str(), retryCount);
  }
  Serial.println("");
  Serial.println("WiFi connected.");

  Serial.print("Gateway: ");
  Serial.println(WiFi.gatewayIP());
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  displayWiFiIP(wifiName.c_str(), wifiPasswd.c_str(),
                WiFi.gatewayIP().toString().c_str(),
                WiFi.localIP().toString().c_str());
}

bool tft_output(int16_t x, int16_t y, uint16_t w, uint16_t h, uint16_t *bitmap)
{
  if (y >= tft.height())
    return 0;
  tft.pushImage(x, y, w, h, bitmap);
  return 1;
}

void initScreen()
{
  TFT_eSPI tft = TFT_eSPI();
  tft.init();
  tft.fillScreen(tft.color565(255, 255, 255));

  TJpgDec.setSwapBytes(true);
  TJpgDec.setCallback(tft_output);
}

void beginTCPServer()
{
  server.setNoDelay(true);
  server.begin(12345);
}

void setup()
{
  Serial.begin(115200);
  echoSystemInfo();

  initScreen();
  configWiFi();
  connectWiFi();

  beginTCPServer();
}

void createFrameReceiver()
{
  if (server.hasClient())
  {
    if (NULL == frameReciver)
    {
      frameReciver = new FrameReceiver(&server);
      Serial.println("new receiver");
    }
    else
    {
      Serial.println("delete receiver by new peer");
      delete frameReciver;
      frameReciver = NULL;
      frameReciver = new FrameReceiver(&server);
      Serial.println("new receiver");
    }
    recvTime = millis();
  }

  if (NULL != frameReciver && !frameReciver->connected())
  {
    Serial.println("receiver disconnect by peer");
    delete frameReciver;
    frameReciver = NULL;
  }
  if (NULL != frameReciver && (millis() - recvTime > 60000))
  {
    Serial.println("receiver disconnect by timeout");
    delete frameReciver;
    frameReciver = NULL;
  }
}

void loop()
{
  uint32_t recvBytes = 0;

  createFrameReceiver();
  if (NULL == frameReciver)
  {
    delay(1);
    return;
  }

  // maybe more than one frame
  while (true)
  {
    recvBytes = frameReciver->recvData();
    if (recvBytes <= 0)
    {
      if (recvBytes < 0)
        Serial.printf("recvBytes < 0\n");
      break;
    }
    recvTime = millis();
    Serial.printf("recvBytes: %d, recvTime: %d\n", recvBytes, recvTime);

    if (frameReciver->hasFrame())
    {
      Serial.printf("drawJpg: skip %d, size %d\n", frameReciver->isSkippedFrame(), frameReciver->frameSize());
      if (!frameReciver->isSkippedFrame())
      {
        // draw it
        TJpgDec.drawJpg(0, 0, frameReciver->frameData(), frameReciver->frameSize());
      }
      frameReciver->nextFrame();
      frameReciver->sendAck();
    }
  }
}
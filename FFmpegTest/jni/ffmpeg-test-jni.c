/**
this is the wrapper of the native functions 
**/
/*android specific headers*/
#include <jni.h>
#include "logjam.h"
#include <android/log.h>
#include <android/bitmap.h>
/*standard library*/
#include <time.h>
#include <math.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <unistd.h>
#include <assert.h>
/*ffmpeg headers*/
#include <libavutil/avstring.h>
#include <libavutil/pixdesc.h>
#include <libavutil/imgutils.h>
#include <libavutil/samplefmt.h>

#include <libavformat/avformat.h>

#include <libswscale/swscale.h>

#include <libavcodec/avcodec.h>
#include <libavformat/url.h>
#include <libavcodec/avfft.h>

/**/
char *gFileName;	  //the file name of the video

AVFormatContext *gFormatCtx;
int gVideoStreamIndex;    //video stream index

AVCodecContext *gVideoCodecCtx;

static void get_video_info(char *prFilename);
void run_decode();
void run_encode();
/*parsing the video file, done by parse thread*/
static void get_video_info(char *prFilename)
{
    AVCodec *lVideoCodec;
    int lError;
    char tempext[10];
    size_t tmpext;
    /*some global variables initialization*/
    LOGI("get video info starts!");
    gFormatCtx = avformat_alloc_context();
    /*register the codec*/
    av_register_all();

    /*register the protocol*/
    extern URLProtocol ff_file_protocol;
    ffurl_register_protocol(&ff_file_protocol, sizeof(ff_file_protocol));
    /*open the video file*/
    if ((lError = avformat_open_input(&gFormatCtx, gFileName, NULL, 0)) !=0 )
    {
        LOGE("Error open video file: %d", lError);
        return;	//open file failed
    }
    /*retrieve stream information*/
    if ((lError = avformat_find_stream_info(gFormatCtx,NULL)) < 0)
    {
        LOGE("Error find stream information: %d", lError);
        return;
    } 
    /*find the video stream and its decoder*/
    gVideoStreamIndex = av_find_best_stream(gFormatCtx, AVMEDIA_TYPE_VIDEO, -1, -1, &lVideoCodec, 0);
    if (gVideoStreamIndex == AVERROR_STREAM_NOT_FOUND) {
        LOGE("Error: cannot find a video stream");
        return;
    }
    else
    {
	LOGI("video codec: %s", lVideoCodec->name);
    }

    if (gVideoStreamIndex == AVERROR_DECODER_NOT_FOUND)
    {
        LOGE("Error: video stream found, but no decoder is found!");
        return;
    }   
    /*open the codec*/
    gVideoCodecCtx = gFormatCtx->streams[gVideoStreamIndex]->codec;
    LOGI( "open codec: (%d, %d)", gVideoCodecCtx->height, gVideoCodecCtx->width);
#ifdef SELECTIVE_DECODING
    gVideoCodecCtx->allow_selective_decoding = 1;
#endif
    if (avcodec_open2(gVideoCodecCtx, lVideoCodec,NULL) < 0)
    {
    	LOGE("Error: cannot open the video codec!");
    		return;
    }
    LOGI("get video info ends");

    tmpext = strlen(gFileName);
    tmpext -=4;
    strcpy(tempext,gFileName +tmpext);
    LOGI("Extention is %s",tempext);

    if(strcmp(tempext,".y4m") == 0)
    {
    	run_encode();
    }
    else
    {
    	run_decode();
    }

}

JNIEXPORT void JNICALL Java_roman10_ffmpegTest_VideoBrowser_naClose(JNIEnv *pEnv, jobject pObj) {
    int l_mbH = (gVideoCodecCtx->height + 15) / 16;
    /*close the video codec*/
    avcodec_close(gVideoCodecCtx);
    /*close the video file*/
    avformat_close_input(&gFormatCtx);
}

JNIEXPORT void JNICALL Java_roman10_ffmpegTest_VideoBrowser_naInit(JNIEnv *pEnv, jobject pObj, jstring pFileName) {
    int l_mbH, l_mbW;
    /*get the video file name*/
    gFileName = (char *)(*pEnv)->GetStringUTFChars(pEnv, pFileName, NULL);
    if (gFileName == NULL)
    {
        LOGE( "Error: cannot get the video file name!");
        return;
    } 
    //LOGI( "video file name is %s", gFileName);
    get_video_info(gFileName);
    //LOGI( "initialization done");
}

JNIEXPORT jstring JNICALL Java_roman10_ffmpegTest_VideoBrowser_naGetVideoCodecName(JNIEnv *pEnv, jobject pObj) {
    const char* lCodecName = gVideoCodecCtx->codec->name;
    return (*pEnv)->NewStringUTF(pEnv, lCodecName);
}

JNIEXPORT jstring JNICALL Java_roman10_ffmpegTest_VideoBrowser_naGetVideoFormatName(JNIEnv *pEnv, jobject pObj) {
    const char* lFormatName = gFormatCtx->iformat->name;
    return (*pEnv)->NewStringUTF(pEnv, lFormatName);
}


JNIEXPORT jintArray JNICALL Java_roman10_ffmpegTest_VideoBrowser_naGetVideoResolution(JNIEnv *pEnv, jobject pObj) {
    jintArray lRes;
    lRes = (*pEnv)->NewIntArray(pEnv, 2);

    if (lRes == NULL)
    {
        LOGI("cannot allocate memory for video size");
        return NULL;
    }

    jint lVideoRes[2];
    lVideoRes[0] = gVideoCodecCtx->width;
    lVideoRes[1] = gVideoCodecCtx->height;
    (*pEnv)->SetIntArrayRegion(pEnv, lRes, 0, 2, lVideoRes);
    return lRes;
}

void run_decode()
{
	int argc = 4;
	char outFile[200];
	char **argv = NULL;
	size_t strleng;
	argv = (char **)(malloc(sizeof(char *)*argc));
	LOGI("run() called");
	argv[1]="-i";
	LOGI("flag is %s",argv[1]);
	argv[2] = gFileName;
	LOGI("input is %s",argv[2]);
	//strleng = strlen(gFileName);
    //temp = substring(1,strleng-4,gFileName);
    //LOGI("%s",temp);
	strleng = strlen(gFileName);
	strleng -= 4;
	strncpy(outFile,gFileName,strleng);
	outFile[strleng] = '\0';
	strcat(outFile,".yuv");
	//argv[3] = "/mnt/sdcard/out.yuv";
	argv[3] = outFile;
	LOGI("outfile is %s",argv[3]);
	LOGI("arg count is %d",argc);
	LOGI("run passing off to main()");
	main(argc, argv);
	LOGI("End of run_decode()");
}


void run_encode()
{
	int argc = 6;
	char outFile[200];
	char **argv = NULL;
	size_t strleng;
	argv = (char **)(malloc(sizeof(char *)*argc));
	LOGI("run_encode() called");
	argv[1]="--qp";
	argv[2]="0";
	LOGI("flag is %s",argv[1]);
	argv[3] = "-o";
	//strleng = strlen(gFileName);
    //temp = substring(1,strleng-4,gFileName);
    //LOGI("%s",temp);
	strleng = strlen(gFileName);
	strleng -= 4;
	strncpy(outFile,gFileName,strleng);
	outFile[strleng] = '\0';
	strcat(outFile,".avi");
	//argv[3] = "/mnt/sdcard/out.yuv";
	argv[4] = outFile;
	LOGI("outfile is %s",argv[4]);
	argv[5] = gFileName;
	LOGI("input is %s",argv[5]);
	LOGI("arg count is %d",argc);
	LOGI("run passing off to main()");
	main_x264(argc, argv);
	LOGI("End of run_encode()");
}


/**
 * @file filename
 * @brief This is a brief description
 * @details This is the detail description
 * @author liuyong
 * @date 2021-4-15
 * @version V0.1
 * @par Copyright by http://eswin.com/.
 * @par History 1:
 *      Date:
 *      Version:
 *      Author:
 *      Modification:
 *
 * @par History 2:
 */


#ifndef _HTTP_OTA_H
#define _HTTP_OTA_H


/*--------------------------------------------------------------------------
* 	                                        	Include files
--------------------------------------------------------------------------*/
#include "ota.h"

/*--------------------------------------------------------------------------
* 	                                        	Macros
--------------------------------------------------------------------------*/
/** Description of the macro */

#define HTTP_DEFAULT_REQ_LEN		2048
#define HTTP_DEFAULT_PORT         	80
#define HTTP_DEFAULT_HEADER_ELN   	1024
#define HTTP_DEFAULT_DOWNLOAD_LEN 	2048
#define HTTP_DEFAULT_TIMEOUT      	10000


/*--------------------------------------------------------------------------
* 	                                        	Types
--------------------------------------------------------------------------*/
/**
 * @brief The brief description
 * @details The detail description
 */
typedef struct
{
    int             port;
    char            *host;                  // malloc��̬�����ڴ棬����ota���ص�ַ�ĵ�����
    char            *path;                  // ��ota upgrade_param��url���ȡ���ִ�
    ip_addr_t       http_server_ip;         // ����host֮��õ���http server��IP��ַ
    int             socket;                 // ����HTTP�����socket
    unsigned char   crlf_num;               // http��crlf(\r\n)����һ�У��������ĸ�/r or /n������http header�Ľ�������Ϊ����ͷ��������֮�����һ��
    char            *http_header_buff;      // malloc��̬�����ڴ棬�����socket��ȡ������HTTP��ͷ
    unsigned short  http_header_count;      // ��¼HTTPͷ�ĳ���
    unsigned int    http_content_length;    // HTTP��Content-Length�ֶΣ���¼����HTTP����Ҫ���͵���������
    unsigned int    rcvd_len;               // �˴�http�����ѻ�ȡ������������
    unsigned short  buff_len;               // �趨ÿ�δ�http socket��ȡ���ݵĴ�С
    unsigned char   *http_rcv_buff;         // malloc��̬�����ڴ棬�����http socket��ȡ������
} http_client_ctx_t;

typedef struct
{
    char 				url[OTA_DEFAULT_BUFF_LEN];   		// �����URL��ַ
    unsigned char       ota_init;           // ��ֹ�ϵ�����ʱ���ٴγ�ʼ��ota
    ota_update_status   update_status;      // OTA�������
    ota_download_status download_status;    // OTA�Ƿ�ʼ���أ������Ƿ����
    ota_error_status    error_status;
    unsigned int        file_size;          // �״η���HTTP����ʱ��server���ص�HTTP��ӦContent-Length�ֶΣ���¼�������İ汾���ļ��ĳ���
    unsigned int        receive_len;        // ���������л�ȡ��������
    unsigned char       first_header;       // ֻ���ڵ�һ���յ�HTTP��������ʱ��������receive_len���ϵ�����ʱ������
    http_client_ctx_t   *http_ctx;          // malloc��̬�����ڴ棬���洦��http�������̵�context
    unsigned char       ota_dowload_time;
} http_instance_t;	

/*--------------------------------------------------------------------------
* 	                                        	Constants
--------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------
* 	                                        	Global  Variables
--------------------------------------------------------------------------*/
/**  Description of global variable  */
extern http_instance_t g_http_instance;

/*--------------------------------------------------------------------------
* 	                                        	Function Prototypes
--------------------------------------------------------------------------*/
int http_client_download_request(void);
void http_client_instance_init(void);
int http_client_download_file(const char *url, unsigned int len);
http_instance_t *get_http_instance(void);
int http_get_port_from_url(char *url);
char *http_get_host_from_url(char *url);





#endif/*_HTTP_OTA_H*/


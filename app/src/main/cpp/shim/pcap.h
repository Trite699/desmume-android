// Minimal libpcap shim for Android (no libpcap available). DeSmuME's wifi.cpp
// compiles against these; the stub implementations in android_platform.cpp make
// SoftAP bridge mode report "unavailable". Local wifi emulation is unaffected.
#pragma once
#include <sys/types.h>
#include <sys/time.h>

#ifndef PCAP_ERRBUF_SIZE
#define PCAP_ERRBUF_SIZE 256
#endif
#define PCAP_OPENFLAG_PROMISCUOUS 1

#ifdef __cplusplus
extern "C" {
#endif
typedef unsigned char u_char;
struct pcap;
struct pcap_pkthdr { struct timeval ts; unsigned int caplen; unsigned int len; };
struct pcap_if { struct pcap_if *next; char *name; char *description; void *addresses; unsigned int flags; };
typedef struct pcap_if pcap_if_t;
typedef void (*pcap_handler)(u_char *, const struct pcap_pkthdr *, const u_char *);

int pcap_findalldevs(pcap_if_t **, char *);
void pcap_freealldevs(pcap_if_t *);
struct pcap *pcap_open_live(const char *, int, int, int, char *);
void pcap_close(struct pcap *);
int pcap_setnonblock(struct pcap *, int, char *);
int pcap_sendpacket(struct pcap *, const u_char *, int);
int pcap_dispatch(struct pcap *, int, pcap_handler, u_char *);
void pcap_breakloop(struct pcap *);
#ifdef __cplusplus
}
#endif

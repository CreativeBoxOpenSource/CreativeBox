#pragma once

#if 0
#define ADS
#include "ads.h"
#else
#define ZIPPWD @"1" // zip password
#endif

#ifdef __cplusplus
extern "C" {
#endif

enum {
	PATH_DOCUMENTS,
	PATH_LIBRARY_SUPPORT,
	PATH_LIBRARY_CACHE,
};

void ioswrap_log(const char *message);
void ioswrap_paths(int type, char *dest, size_t destlen);
void ioswrap_assets(void); // extracts assets.zip to PATH_LIBRARY_SUPPORT
void ioswrap_asset_refresh(void);
void ioswrap_size(unsigned int *dest);
void ioswrap_show_dialog(void *uiviewcontroller, const char *accept, const char *hint, const char *current, int type);
int  ioswrap_get_dialog(const char **text);

#ifdef __cplusplus
}
#endif

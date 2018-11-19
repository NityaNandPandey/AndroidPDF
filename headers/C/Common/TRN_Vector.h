//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
#ifndef PDFTRON_H_CCommonVector
#define PDFTRON_H_CCommonVector

#ifdef __cplusplus
extern "C" {
#endif

#include <C/Common/TRN_Types.h>

	TRN_API TRN_VectorGetSize(TRN_Vector* vector, TRN_UInt32* out_size);
	TRN_API TRN_VectorGetData(TRN_Vector* vector, void** out_ptr);
	TRN_API TRN_VectorGetAt(TRN_Vector* vector, TRN_UInt32 pos, void** out_value);
	TRN_API TRN_VectorDestroy(TRN_Vector* vector);

#ifdef __cplusplus
}
#endif

#endif // PDFTRON_H_CCommonVector

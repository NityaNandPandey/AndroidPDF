//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
// !Warning! This file is autogenerated, modify the .codegen file, not this one
// (any changes here will be wiped out during the autogen process)

#ifndef PDFTRON_H_CCubicCurveBuilder
#define PDFTRON_H_CCubicCurveBuilder

#ifdef __cplusplus
extern "C" {
#endif

#include <C/Common/TRN_Types.h>
#include <C/Common/TRN_Exception.h>


struct TRN_CubicCurveBuilder_tag;
typedef struct TRN_CubicCurveBuilder_tag* TRN_CubicCurveBuilder;


/* methods: */
TRN_API TRN_CubicCurveBuilderNumSourcePoints(TRN_CubicCurveBuilder self, TRN_UInt32* result);
TRN_API TRN_CubicCurveBuilderAddSourcePoint(TRN_CubicCurveBuilder self, double x, double y);
TRN_API TRN_CubicCurveBuilderNumCubicPoints(TRN_CubicCurveBuilder self, TRN_UInt32* result);
TRN_API TRN_CubicCurveBuilderGetCubicXCoord(TRN_CubicCurveBuilder self, TRN_UInt32 index, double* result);
TRN_API TRN_CubicCurveBuilderGetCubicYCoord(TRN_CubicCurveBuilder self, TRN_UInt32 index, double* result);

TRN_API TRN_CubicCurveBuilderDestroy(TRN_CubicCurveBuilder* self);


#ifdef __cplusplus
} // extern C
#endif

#endif /* PDFTRON_H_CCubicCurveBuilder */

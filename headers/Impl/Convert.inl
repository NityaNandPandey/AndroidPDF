void SetFlattenContentImpl(TRN_Obj obj, enum Convert::FlattenFlag flatten, TRN_Obj* result);
void SetFlattenThresholdImpl(TRN_Obj obj, enum Convert::FlattenThresholdFlag threshold, TRN_Obj* result);
void SetOverprintImpl(TRN_Obj obj, enum PDFRasterizer::OverprintPreviewMode mode, TRN_Obj* result);

inline void Convert::FromXps(PDFDoc & in_pdfdoc, const UString & in_filename)
{
	REX(TRN_ConvertFromXps(in_pdfdoc.mp_doc, in_filename.mp_impl));
}

inline void Convert::FromXps(PDFDoc & in_pdfdoc, const char* buf, size_t buf_sz)
{
	REX(TRN_ConvertFromXpsMem(in_pdfdoc.mp_doc, buf, buf_sz));
}

inline void Convert::FromEmf(PDFDoc & in_pdfdoc, const UString & in_filename)
{
	REX(TRN_ConvertFromEmf(in_pdfdoc.mp_doc, in_filename.mp_impl));
}

inline void Convert::FromText(PDFDoc& in_pdfdoc, const UString& in_filename, const SDF::Obj& options)
{
	REX(TRN_ConvertFromText(in_pdfdoc.mp_doc, in_filename.mp_impl, options.mp_obj));
}

inline void Convert::ToEmf(PDFDoc & in_pdfdoc, const UString & in_filename)
{
	REX(TRN_ConvertDocToEmf(in_pdfdoc.mp_doc, in_filename.mp_impl));
}

inline void Convert::ToEmf(Page & in_page, const UString & in_filename)
{
	REX(TRN_ConvertPageToEmf(in_page.mp_page, in_filename.mp_impl));
}

inline Convert::SVGOutputOptions::SVGOutputOptions()
{
	m_obj = m_objset.CreateDict().mp_obj;
}

inline void Convert::SVGOutputOptions::SetEmbedImages(bool embed_images)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "EMBEDIMAGES", BToTB(embed_images), &result));
}

inline void Convert::SVGOutputOptions::SetNoFonts(bool no_fonts)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "NOFONTS", BToTB(no_fonts), &result));
}

inline void Convert::SVGOutputOptions::SetSvgFonts(bool svg_fonts)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "SVGFONTS", BToTB(svg_fonts), &result));
}

inline void Convert::SVGOutputOptions::SetEmbedFonts(bool embed_fonts)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "EMBEDFONTS", BToTB(embed_fonts), &result));
}

inline void Convert::SVGOutputOptions::SetFlattenContent(enum FlattenFlag flatten)
{
	TRN_Obj result;
	SetFlattenContentImpl(m_obj, flatten, &result);
}

inline void Convert::SVGOutputOptions::SetFlattenThreshold(enum FlattenThresholdFlag threshold)
{
	TRN_Obj result;
	SetFlattenThresholdImpl(m_obj, threshold, &result);
}

inline void Convert::SVGOutputOptions::SetFlattenDPI(UInt32 dpi)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj, "DPI", dpi, &result));
}

inline void Convert::SVGOutputOptions::SetFlattenMaximumImagePixels(UInt32 max_pixels)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj, "MAX_IMAGE_PIXELS", max_pixels, &result));
}

inline void Convert::SVGOutputOptions::SetCompress(bool svgz)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "SVGZ", BToTB(svgz), &result));
}

inline void Convert::SVGOutputOptions::SetOutputThumbnails(bool include_thumbs)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"NOTHUMBS",BToTB(!include_thumbs),&result));
}

inline void Convert::SVGOutputOptions::SetThumbnailSize(UInt32 size)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"THUMB_SIZE",size,&result));
}

inline void Convert::SVGOutputOptions::SetCreateXmlWrapper(bool xml)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "NOXMLDOC", BToTB(!xml), &result));
}

inline void Convert::SVGOutputOptions::SetDtd(bool dtd)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "OMITDTD", BToTB(!dtd), &result));
}

inline void Convert::SVGOutputOptions::SetAnnots(bool annots)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "NOANNOTS", BToTB(!annots), &result));
}

inline void Convert::SVGOutputOptions::SetNoUnicode(bool no_unicode)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "NOUNICODE", BToTB(no_unicode), &result));
}

inline void Convert::SVGOutputOptions::SetIndividualCharPlacement(bool individual_char_placement)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "INDIVIDUALCHARPLACEMENT", BToTB(individual_char_placement), &result));
}

inline void Convert::SVGOutputOptions::SetRemoveCharPlacement(bool remove_char_placement)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "REMOVECHARPLACEMENT", BToTB(remove_char_placement), &result));
}

inline void Convert::SVGOutputOptions::SetOverprint(PDFRasterizer::OverprintPreviewMode mode)
{
	TRN_Obj result;
	SetOverprintImpl(m_obj, mode, &result);
}

inline void Convert::ToSvg(PDFDoc & in_pdfdoc, const UString & in_filename, const Convert::SVGOutputOptions& in_options)
{
	REX(TRN_ConvertDocToSvgWithOptions(in_pdfdoc.mp_doc, in_filename.mp_impl, in_options.m_obj));
}

inline void Convert::ToSvg(Page & in_page, const UString & in_filename, const Convert::SVGOutputOptions& in_options)
{
	REX(TRN_ConvertPageToSvgWithOptions(in_page.mp_page, in_filename.mp_impl, in_options.m_obj));
}

inline Convert::XPSOutputCommonOptions::XPSOutputCommonOptions()
{
	m_obj=m_objset.CreateDict().mp_obj;
}

inline void Convert::XPSOutputCommonOptions::SetPrintMode(bool print_mode)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"PRINTMODE",BToTB(print_mode),&result));
}

inline void Convert::XPSOutputCommonOptions::SetDPI(UInt32 dpi)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"DPI",dpi,&result));
}

inline void Convert::XPSOutputCommonOptions::SetRenderPages(bool render)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"RENDER",BToTB(render),&result));
}

inline void Convert::XPSOutputCommonOptions::SetThickenLines(bool thicken)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"THICKENLINES",BToTB(thicken),&result));
}

inline void Convert::XPSOutputCommonOptions::GenerateURLLinks(bool generate)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"URL_LINKS",BToTB(generate),&result));
}

inline void Convert::XPSOutputCommonOptions::SetOverprint(enum OverprintPreviewMode mode)
{
	TRN_Obj result;
	SetOverprintImpl(m_obj, static_cast<PDFRasterizer::OverprintPreviewMode>(mode), &result);
}

inline void Convert::XPSOutputOptions::SetOpenXps(bool openxps)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"OPENXPS",BToTB(openxps),&result));
}

inline void Convert::XODOutputOptions::SetOutputThumbnails(bool include_thumbs)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"NOTHUMBS",BToTB(!include_thumbs),&result));
}

inline void Convert::XODOutputOptions::SetThumbnailSize(UInt32 size)
{
	SetThumbnailSize(size,size);
}

inline void Convert::XODOutputOptions::SetThumbnailSize(UInt32 regular_size, UInt32 large_size)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"THUMB_SIZE",regular_size,&result));
	REX(TRN_ObjPutNumber(m_obj,"LARGE_THUMB_SIZE",large_size,&result));

}

inline void Convert::XODOutputOptions::SetElementLimit(UInt32 element_limit)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"ELEMENTLIMIT",element_limit,&result));
}

inline void Convert::XODOutputOptions::SetOpacityMaskWorkaround(bool opacity_render)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"MASKRENDER",opacity_render,&result));
}

inline void Convert::XODOutputOptions::SetMaximumImagePixels(UInt32 max_pixels)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"MAX_IMAGE_PIXELS",max_pixels,&result));
}

inline void Convert::XODOutputOptions::SetFlattenContent(enum FlattenFlag flatten)
{
	TRN_Obj result;
	SetFlattenContentImpl(m_obj, flatten, &result);
}

inline void Convert::XODOutputOptions::SetFlattenThreshold(enum FlattenThresholdFlag threshold)
{
	TRN_Obj result;
	SetFlattenThresholdImpl(m_obj, threshold, &result);
}

inline void Convert::XODOutputOptions::SetPreferJPG(bool prefer_jpg)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"PREFER_JPEG",prefer_jpg,&result));
}

inline void Convert::XODOutputOptions::SetJPGQuality(UInt32 quality)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"JPEG_QUALITY",quality,&result));
}

inline void Convert::XODOutputOptions::SetSilverlightTextWorkaround(bool workaround)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"REMOVE_ROTATED_TEXT",workaround,&result));
}

inline void Convert::XODOutputOptions::SetAnnotationOutput(enum AnnotationOutputFlag annot_output)
{
	TRN_Obj result;
	switch (annot_output)
	{
		case e_internal_xfdf:
			REX(TRN_ObjPutName(m_obj, "ANNOTATION_OUTPUT", "INTERNAL", &result));
			break;
		case e_external_xfdf:
			REX(TRN_ObjPutName(m_obj, "ANNOTATION_OUTPUT", "EXTERNAL", &result));
			break;
		case e_flatten:
			REX(TRN_ObjPutName(m_obj, "ANNOTATION_OUTPUT", "FLATTEN", &result));
			break;
	}
}

inline void Convert::XODOutputOptions::SetExternalParts(bool generate)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "EXTERNAL_PARTS", generate, &result));
}

inline void Convert::XODOutputOptions::SetEncryptPassword(const char* pass)
{
	TRN_Obj result;
	REX(TRN_ObjPutName(m_obj, "ENCRYPT_PASSWORD", pass, &result));
}

inline void Convert::XODOutputOptions::UseSilverlightFlashCompatible(bool compatible)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "COMPATIBLE_XOD", compatible, &result));
}

inline Convert::HTMLOutputOptions::HTMLOutputOptions()
{
	m_obj=m_objset.CreateDict().mp_obj;
}

inline void Convert::HTMLOutputOptions::SetPreferJPG(bool prefer_jpg)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"PREFER_JPEG",prefer_jpg,&result));
}

inline void Convert::HTMLOutputOptions::SetJPGQuality(UInt32 quality)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"JPEG_QUALITY",quality,&result));
}


inline void Convert::HTMLOutputOptions::SetDPI(UInt32 dpi)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"DPI",dpi,&result));
}

inline void Convert::HTMLOutputOptions::SetMaximumImagePixels(UInt32 max_pixels)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"MAX_IMAGE_PIXELS",max_pixels,&result));
}

inline void Convert::HTMLOutputOptions::SetReflow(bool reflow)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"REFLOW",reflow,&result));
}

inline void Convert::HTMLOutputOptions::SetScale(double scale)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"SCALE",scale,&result));
}

inline void Convert::HTMLOutputOptions::SetExternalLinks(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"EXTERNAL_LINKS",enable,&result));
}

inline void Convert::HTMLOutputOptions::SetInternalLinks(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"INTERNAL_LINKS",enable,&result));
}

inline void Convert::HTMLOutputOptions::SetSimplifyText(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"SIMPLIFY_TEXT",enable,&result));
}

inline void Convert::HTMLOutputOptions::SetReportFile(const UString& path)
{
	TRN_Obj result;
	REX(TRN_ObjPutText(m_obj,"REPORT_FILE",path.mp_impl,&result));
}

inline Convert::TiffOutputOptions::TiffOutputOptions()
{
	m_obj=m_objset.CreateDict().mp_obj;
}

inline void Convert::TiffOutputOptions::SetBox(enum Page::Box type)
{
	TRN_Obj result;
	switch (type)
	{
	case e_Page_media:
		REX(TRN_ObjPutName(m_obj, "BOX", "media", &result));
		break;
	case e_Page_crop:
		REX(TRN_ObjPutName(m_obj, "BOX", "crop", &result));
		break;
	case e_Page_bleed:
		REX(TRN_ObjPutName(m_obj, "BOX", "bleed", &result));
		break;
	case e_Page_trim:
		REX(TRN_ObjPutName(m_obj, "BOX", "trim", &result));
		break;
	case e_Page_art:
		REX(TRN_ObjPutName(m_obj, "BOX", "art", &result));
		break;
	}
}

inline void Convert::TiffOutputOptions::SetRotate(enum Page::Rotate rotation)
{
	TRN_Obj result;
	switch (rotation)
	{
	case e_Page_0:
		REX(TRN_ObjPutName(m_obj, "ROTATE", "0", &result));
		break;
	case e_Page_90:
		REX(TRN_ObjPutName(m_obj, "ROTATE", "90", &result));
		break;
	case e_Page_180:
		REX(TRN_ObjPutName(m_obj, "ROTATE", "180", &result));
		break;
	case e_Page_270:
		REX(TRN_ObjPutName(m_obj, "ROTATE", "270", &result));
		break;
	}
}

inline void Convert::TiffOutputOptions::SetClip(double x1, double y1, double x2, double y2)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"CLIP_X1",x1,&result));
	REX(TRN_ObjPutNumber(m_obj,"CLIP_Y1",y1,&result));
	REX(TRN_ObjPutNumber(m_obj,"CLIP_X2",x2,&result));
	REX(TRN_ObjPutNumber(m_obj,"CLIP_Y2",y2,&result));
}

inline void Convert::TiffOutputOptions::SetPages(const char* page_desc)
{
	TRN_Obj result;
	REX(TRN_ObjPutName(m_obj, "PAGES", page_desc, &result));
}

inline void Convert::TiffOutputOptions::SetOverprint(enum PDFRasterizer::OverprintPreviewMode mode)
{
	TRN_Obj result;
	SetOverprintImpl(m_obj, mode, &result);
}

inline void Convert::TiffOutputOptions::SetCMYK(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "CMYK", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetDither(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "DITHER", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetGray(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "GRAY", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetMono(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "MONO", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetAnnots(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "ANNOTS", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetSmooth(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "SMOOTH", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetPrintmode(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "PRINTMODE", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetTransparentPage(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "TRANSPARENT_PAGE", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetPalettized(bool enable)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj, "PALETTIZED", BToTB(enable), &result));
}

inline void Convert::TiffOutputOptions::SetDPI(double dpi)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"DPI",dpi,&result));
}

inline void Convert::TiffOutputOptions::SetGamma(double gamma)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"GAMMA",gamma,&result));
}

inline void Convert::TiffOutputOptions::SetHRes(int hres)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"HRES",static_cast<double>(hres),&result));
}

inline void Convert::TiffOutputOptions::SetVRes(int vres)
{
	TRN_Obj result;
	REX(TRN_ObjPutNumber(m_obj,"VRES",static_cast<double>(vres),&result));
}

inline Convert::EPUBOutputOptions::EPUBOutputOptions()
{
	m_obj=m_objset.CreateDict().mp_obj;
}

inline void Convert::EPUBOutputOptions::SetExpanded(bool expanded)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"EPUB_EXPANDED",expanded,&result));
}

inline void Convert::EPUBOutputOptions::SetReuseCover(bool reuse)
{
	TRN_Obj result;
	REX(TRN_ObjPutBool(m_obj,"EPUB_REUSE_COVER",reuse,&result));
}

inline void Convert::ToXps( PDFDoc & in_pdfdoc, const UString & in_filename, const Convert::XPSOutputOptions& options)
{
	REX(TRN_ConvertToXps(in_pdfdoc.mp_doc,in_filename.mp_impl, options.m_obj));
}

inline void Convert::ToXps( const UString & in_inputFilename, const UString & in_outputFilename, const Convert::XPSOutputOptions& options)
{
	REX(TRN_ConvertFileToXps(in_inputFilename.mp_impl, in_outputFilename.mp_impl, options.m_obj));
}

inline void Convert::ToXod( const UString & in_filename, const UString & out_filename, const Convert::XODOutputOptions& options)
{
	REX(TRN_ConvertFileToXod(in_filename.mp_impl, out_filename.mp_impl, options.m_obj));
}

inline void Convert::ToXod(PDFDoc & in_pdfdoc, const UString & out_filename, const Convert::XODOutputOptions& options)
{
	REX(TRN_ConvertToXod(in_pdfdoc.mp_doc, out_filename.mp_impl, options.m_obj));
}

inline Filters::Filter Convert::ToXod( const UString & in_filename, const Convert::XODOutputOptions& options)
{
	TRN_Filter result;
	REX(TRN_ConvertFileToXodStream(in_filename.mp_impl, options.m_obj, &result));
	return Filters::Filter(result,true);
}

inline Filters::Filter Convert::ToXod(PDFDoc& in_pdfdoc, const Convert::XODOutputOptions& options)
{
	TRN_Filter result;
	REX(TRN_ConvertToXodStream(in_pdfdoc.mp_doc, options.m_obj, &result)); // try
	return Filters::Filter(result,true);
}

inline ConversionMonitor Convert::ToXodWithMonitor(PDFDoc& in_pdfdoc, const Convert::XODOutputOptions& options)
{
	TRN_ConversionMonitor result;
	REX(TRN_ConvertToXodWithMonitor(in_pdfdoc.mp_doc, options.m_obj, &result));
	ConversionMonitor monitor = ConversionMonitor(result,true);
	return monitor;
}

inline ConversionMonitor::ConversionMonitor(TRN_ConversionMonitor impl, bool is_owner) {
	mp_impl=impl;
	m_owner=is_owner;
}
inline ConversionMonitor::ConversionMonitor() : mp_impl(0), m_owner(true) {}

inline bool ConversionMonitor::Next()
{
	RetBool(TRN_ConversionMonitorNext(mp_impl,&result));
}

inline bool ConversionMonitor::Ready()
{
	RetBool(TRN_ConversionMonitorReady(mp_impl,&result));
}

inline UInt32 ConversionMonitor::Progress()
{
	UInt32 result;
	REX(TRN_ConversionMonitorProgress(mp_impl,&result));
	return result;
}

inline Filters::Filter ConversionMonitor::Filter()
{
	TRN_Filter result;
	REX(TRN_ConversionMonitorFilter(mp_impl, &result));
	return Filters::Filter(result,true);
}

inline ConversionMonitor::ConversionMonitor(const ConversionMonitor& copy) 
{
	if(copy.m_owner) {
		((ConversionMonitor&)copy).m_owner = false;
		m_owner=true;
	}
	else {
		m_owner=false;
	}

	mp_impl=copy.mp_impl;
}

inline ConversionMonitor& ConversionMonitor::operator =(const ConversionMonitor& other) 
{
	if(m_owner) { 
		REX(TRN_ConversionMonitorDestroy(mp_impl));
		mp_impl=0;
	}

	if(other.m_owner) {
		((ConversionMonitor&)other).m_owner = false;
		m_owner=true;
	}
	else {
		m_owner=false;
	}

	mp_impl=other.mp_impl;
	return *this;
}
inline ConversionMonitor::~ConversionMonitor()
{
	if(m_owner) {
		DREX(mp_impl, TRN_ConversionMonitorDestroy(mp_impl));
	}
}

inline void ConversionMonitor::Destroy()
{
	if(m_owner) {
		REX(TRN_ConversionMonitorDestroy(mp_impl));
		mp_impl = 0;
	}
}

inline void Convert::ToHtml(const UString & in_filename, const UString & out_path, const HTMLOutputOptions& options)
{
	REX(TRN_ConvertFileToHtml(in_filename.mp_impl, out_path.mp_impl, options.m_obj));
}

inline void Convert::ToHtml(PDFDoc & in_pdfdoc, const UString & out_path, const HTMLOutputOptions& options)
{
	REX(TRN_ConvertToHtml(in_pdfdoc.mp_doc, out_path.mp_impl, options.m_obj));
}

inline void Convert::ToTiff(const UString & in_filename, const UString & out_path, const Convert::TiffOutputOptions& options)
{
	REX(TRN_ConvertFileToTiff(in_filename.mp_impl, out_path.mp_impl, options.m_obj));
}

inline void Convert::ToTiff(PDFDoc & in_pdfdoc, const UString & out_path, const Convert::TiffOutputOptions& options)
{
	REX(TRN_ConvertToTiff(in_pdfdoc.mp_doc, out_path.mp_impl, options.m_obj));
}

inline void Convert::ToEpub(const UString & in_filename, const UString & out_path, const HTMLOutputOptions& html_options, const Convert::EPUBOutputOptions& epub_options)
{
	REX(TRN_ConvertFileToEpub(in_filename.mp_impl, out_path.mp_impl, html_options.m_obj, epub_options.m_obj));
}

inline void Convert::ToEpub(PDFDoc & in_pdfdoc, const UString & out_path, const HTMLOutputOptions& html_options, const Convert::EPUBOutputOptions& epub_options)
{
	REX(TRN_ConvertToEpub(in_pdfdoc.mp_doc, out_path.mp_impl, html_options.m_obj, epub_options.m_obj));
}

inline DocumentConversion Convert::WordToPDFConversion(PDFDoc & in_pdfdoc, const UString & in_filename, WordToPDFOptions* options)
{
	DocumentConversion ret((TRN_DocumentConversion)0);
	// a pointer to a pointer to an sdf doc
	PDFDoc* ptr = &in_pdfdoc;
	// a pointer to a pointer to an sdf doc
	TRN_PDFDoc* to_pass = (TRN_PDFDoc*)ptr;
	TRN_Obj opt_ptr = options ? options->GetInternalObj().mp_obj : 0;
	REX(TRN_ConvertWordToPdfConversion(to_pass, in_filename.mp_impl, opt_ptr ,&ret.m_impl));
	return ret;
}

inline void Convert::WordToPDF(PDFDoc & in_pdfdoc, const UString & in_filename, WordToPDFOptions* options)
{
	TRN_Obj opt_ptr = options ? options->GetInternalObj().mp_obj : 0;
	REX(TRN_ConvertWordToPdf(in_pdfdoc.mp_doc, in_filename.mp_impl, opt_ptr));
}

inline DocumentConversion Convert::WordToPDFConversion(PDFDoc & in_pdfdoc, Filters::Filter in_data, WordToPDFOptions* options)
{
	DocumentConversion ret((TRN_DocumentConversion)0);
	// a pointer to a pointer to an sdf doc
	PDFDoc* ptr = &in_pdfdoc;
	// a pointer to a pointer to an sdf doc
	TRN_PDFDoc* to_pass = (TRN_PDFDoc*)ptr;
	TRN_Obj opt_ptr = options ? options->GetInternalObj().mp_obj : 0;
	// make sure the filter doesn't die as it leaves this method scope (the conversion will take ownership)
	in_data.m_owner = false;
	REX(TRN_ConvertWordToPdfConversionWithFilter(to_pass, in_data.m_impl, opt_ptr ,&ret.m_impl));
	return ret;
}

inline void Convert::WordToPDF(PDFDoc & in_pdfdoc, Filters::Filter in_data, WordToPDFOptions* options)
{
	TRN_Obj opt_ptr = options ? options->GetInternalObj().mp_obj : 0;
	// make sure the filter doesn't double-delete as it leaves this method scope 
	// (the conversion will take care of deletion)
	in_data.m_owner = false;
	REX(TRN_ConvertWordToPdfWithFilter(in_pdfdoc.mp_doc, in_data.m_impl, opt_ptr));
}

inline void Convert::ToPdf(PDFDoc & in_pdfdoc, const UString & in_filename)
{
	REX(TRN_ConvertToPdf(in_pdfdoc.mp_doc, in_filename.mp_impl));
}

inline bool Convert::RequiresPrinter(const UString & in_filename)
{
	RetBool(TRN_ConvertRequiresPrinter(in_filename.mp_impl, &result));
}

inline void Convert::Printer::Install(const UString & in_printerName)
{
	REX(TRN_ConvertPrinterInstall(in_printerName.mp_impl));
}

inline void Convert::Printer::Uninstall()
{
	REX(TRN_ConvertPrinterUninstall());
}

inline const UString Convert::Printer::GetPrinterName()
{
	RetStr(TRN_ConvertPrinterGetPrinterName(&result));
}

inline void Convert::Printer::SetPrinterName(const UString & in_printerName)
{
	REX(TRN_ConvertPrinterSetPrinterName(in_printerName.mp_impl));
}

inline bool Convert::Printer::IsInstalled(const UString & in_printerName)
{
	RetBool(TRN_ConvertPrinterIsInstalled(in_printerName.mp_impl,&result));
}

inline void Convert::Printer::SetMode(Mode mode)
{
	REX(TRN_ConvertPrinterSetMode((enum TRN_ConvertPrinterMode)mode));
}

inline Convert::Printer::Mode Convert::Printer::GetMode()
{
	enum TRN_ConvertPrinterMode result;
	REX(TRN_ConvertPrinterGetMode(&result));
	return (Convert::Printer::Mode)result;
}

void SetFlattenContentImpl(TRN_Obj obj, enum Convert::FlattenFlag flatten, TRN_Obj* result)
{
	switch (flatten)
	{
		case Convert::e_off:
			REX(TRN_ObjPutName(obj, "FLATTEN_CONTENT", "OFF", result));
			break;
		case Convert::e_simple:
			REX(TRN_ObjPutName(obj, "FLATTEN_CONTENT", "SIMPLE", result));
			break;
		case Convert::e_fast:
			REX(TRN_ObjPutName(obj, "FLATTEN_CONTENT", "FAST", result));
			break;
		case Convert::e_high_quality:
			REX(TRN_ObjPutName(obj, "FLATTEN_CONTENT", "HIGH_QUALITY", result));
	}	
}

void SetFlattenThresholdImpl(TRN_Obj obj, enum Convert::FlattenThresholdFlag threshold, TRN_Obj* result)
{
	switch (threshold)
	{
		case Convert::e_very_strict:
			REX(TRN_ObjPutName(obj, "FLATTEN_THRESHOLD", "VERY_STRICT", result));
			break;
		case Convert::e_strict:
			REX(TRN_ObjPutName(obj, "FLATTEN_THRESHOLD", "STRICT", result));
			break;
		case Convert::e_default:
			REX(TRN_ObjPutName(obj, "FLATTEN_THRESHOLD", "DEFAULT", result));
			break;
		case Convert::e_keep_most:
			REX(TRN_ObjPutName(obj, "FLATTEN_THRESHOLD", "KEEP_MOST", result));
			break;
		case Convert::e_keep_all:
			REX(TRN_ObjPutName(obj, "FLATTEN_THRESHOLD", "KEEP_ALL", result));
			break;
	}
}

void SetOverprintImpl(TRN_Obj obj, enum PDFRasterizer::OverprintPreviewMode mode, TRN_Obj* result)
{
	switch (mode)
	{
		case PDFRasterizer::e_op_off:
			REX(TRN_ObjPutName(obj, "OVERPRINT_MODE", "OFF", result));
			break;
		case PDFRasterizer::e_op_on:
			REX(TRN_ObjPutName(obj, "OVERPRINT_MODE", "ON", result));
			break;
		case PDFRasterizer::e_op_pdfx_on:
			REX(TRN_ObjPutName(obj, "OVERPRINT_MODE", "PDFX", result));
			break;
	}
}

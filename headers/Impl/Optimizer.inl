//IMAGE SETTINGS
//CONSTRUCTORS
inline Optimizer::ImageSettings::ImageSettings()
{
	TRN_OptimizerImageSettingsInit((TRN_OptimizerImageSettings*)this);
}

//FUNCTIONS
inline void Optimizer::ImageSettings::SetImageDPI(double maximum,double resampling)
{
	m_max_dpi = maximum;
	m_resample_dpi = resampling;
}

inline void Optimizer::ImageSettings::SetCompressionMode(enum CompressionMode mode)
{
	m_compression_mode = (enum TRN_Optimizer_ImageSettings_CompressionMode)mode;
}

inline void Optimizer::ImageSettings::SetDownsampleMode(enum DownsampleMode mode)
{
	m_downsample_mode = (enum TRN_Optimizer_ImageSettings_DownsampleMode)mode;
}

inline void Optimizer::ImageSettings::SetQuality(UInt32 quality)
{
	m_quality = quality;
}

inline void Optimizer::ImageSettings::ForceRecompression(bool force)
{
	m_force_recompression = force;
}

inline void Optimizer::ImageSettings::ForceChanges(bool force)
{
	m_force_changes = force;
}

//MONO IMAGE SETTINGS
//CONSTRUCTORS
inline Optimizer::MonoImageSettings::MonoImageSettings()
{
	TRN_OptimizerMonoImageSettingsInit((TRN_OptimizerMonoImageSettings*)this);
}

//FUNCTIONS
inline void Optimizer::MonoImageSettings::SetImageDPI(double maximum,double resampling)
{
	m_max_dpi = maximum;
	m_resample_dpi = resampling;
}

inline void Optimizer::MonoImageSettings::SetCompressionMode(enum CompressionMode mode)
{
	m_compression_mode = (enum TRN_Optimizer_MonoImageSettings_CompressionMode)mode;
}

inline void Optimizer::MonoImageSettings::SetDownsampleMode(enum DownsampleMode mode)
{
	m_downsample_mode = (enum TRN_Optimizer_MonoImageSettings_DownsampleMode)mode;
}

inline void Optimizer::MonoImageSettings::ForceRecompression(bool force)
{
	m_force_recompression = force;
}

inline void Optimizer::MonoImageSettings::ForceChanges(bool force)
{
	m_force_changes = force;
}

inline void Optimizer::MonoImageSettings::SetJBIG2Threshold(double jbig2_threshold)
{
	m_jbig2_threshold = jbig2_threshold;
}

inline Optimizer::TextSettings::TextSettings()
{
	TRN_OptimizerTextSettingsInit((TRN_OptimizerTextSettings*)this);
}

inline void Optimizer::TextSettings::SubsetFonts(bool subset)
{
	m_subset_fonts = subset;
}


inline void Optimizer::TextSettings::EmbedFonts(bool embed)
{
	m_embed_fonts = embed;
}



inline Optimizer::OptimizerSettings::OptimizerSettings() : m_remove_custom(true)
{

}
	
inline void Optimizer::OptimizerSettings::SetColorImageSettings(const Optimizer::ImageSettings& settings)
{
	m_color_image_settings=settings;
}

inline void Optimizer::OptimizerSettings::SetGrayscaleImageSettings(const Optimizer::ImageSettings& settings)
{
	m_grayscale_image_settings=settings;
}

inline void Optimizer::OptimizerSettings::SetMonoImageSettings(const Optimizer::MonoImageSettings& settings)
{
	m_mono_image_settings=settings;
}
	
inline void Optimizer::OptimizerSettings::SetTextSettings(const Optimizer::TextSettings& settings)
{
	m_text_settings=settings;
}

inline void Optimizer::OptimizerSettings::RemoveCustomEntries(bool should_remove)
{
	m_remove_custom=should_remove;
}


//OPTIMIZER
inline void Optimizer::Optimize(PDFDoc& doc, const OptimizerSettings& settings)
{
	REX(TRN_OptimizerOptimize(doc.mp_doc,
		(const TRN_OptimizerImageSettings*)&settings.m_color_image_settings,
		(const TRN_OptimizerImageSettings*)&settings.m_grayscale_image_settings,
		(const TRN_OptimizerMonoImageSettings*)&settings.m_mono_image_settings,
		(const TRN_OptimizerTextSettings*)&settings.m_text_settings,BToTB(settings.m_remove_custom)));
}

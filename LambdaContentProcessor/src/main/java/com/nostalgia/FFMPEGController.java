package com.nostalgia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/*
 * Here's what we are trying to replace here:
 * 	final Process p = Runtime
				.getRuntime()
				.exec("ffmpeg -i http://odna.octoshape.net/f3f5m2v4/cds/ch4_320p/chunklist.m3u8 -g 52 -acodec libfdk_aac "
						+ "-ab 64k -profile:a aac_he -frag_size 4096000 -frag_duration 100000 -vn -vcodec libx264 -vb 448k -f "
						+ "mp4 -movflags frag_keyframe+empty_moov test_audio.mp4 "
						+ "-vcodec libx264 -vb 448k -an -g 52 -f mp4 -movflags frag_keyframe+empty_moov test_video.mp4");
 * 
 */
import java.util.Map;




public class FFMPEGController {

	public void installBinaries(boolean overwrite)
	{
		final URL packagedFFMPEGURL  = getClass().getClassLoader().getResource("ffmpeg"); 
		mffmpegBin = installBinary(packagedFFMPEGURL, new File("/tmp"), "ffmpeg", overwrite);
	}

	public String getBinaryPath ()
	{
		return mffmpegBin;
	}

	private static String installBinary(URL resourceURL, File parent, String filename, boolean upgrade) {
		parent.mkdirs();
		try {
			File f = new File(parent, filename);

			boolean exists = false; 
			if (f.exists()) {
				exists = true;
				if(upgrade){
					f.delete();
				}
			}

			if(!exists || upgrade){
				copyRawFile(resourceURL, f, "555");
			}
			return f.getCanonicalPath();
		} catch (Exception e) {
			System.err.println( "installBinary failed: " + e.getLocalizedMessage());
			return null;
		}
	}

	/**
	 * Copies a raw resource file, given its ID to the given location
	 * @param ctx context
	 * @param resid resource id
	 * @param file destination file
	 * @param mode file permissions (E.g.: "755")
	 * @throws IOException on error
	 * @throws InterruptedException when interrupted
	 */
	private static void copyRawFile(URL sourceFileURL, File file, String mode) throws IOException, InterruptedException
	{
		final String abspath = file.getAbsolutePath();

		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = sourceFileURL.openStream();  
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod "+mode+" "+abspath).waitFor();
	}



	//public static final String mffmpegBin = "bin/ffmpeg";
	public static String mffmpegBin;

	public class Argument
	{
		String key;
		String value;

		public static final String VIDEOCODEC = "-vcodec";
		public static final String AUDIOCODEC = "-acodec";

		public static final String VIDEOBITSTREAMFILTER = "-vbsf";
		public static final String AUDIOBITSTREAMFILTER = "-absf";

		public static final String VERBOSITY = "-v";
		public static final String FILE_INPUT = "-i";
		public static final String ENABLE_REALTIME = "-re";
		public static final String KEYFRAME_INTERVAL = "-g";
		public static final String AUDIO_BITRATE = "-ab";
		public static final String AUDIO_PROFILE = "-profile:a";
		public static final String FRAGMENT_SIZE = "-frag_size";
		public static final String FRAGMENT_DUR = "-frag_duration";
		public static final String DISABLE_VIDEO = "-vn";
		public static final String VIDEO_BITRATE = "-vb";
		public static final String VIDEO_PROFILE = "-profile:v";
		public static final String MOVFLAGS = "-movflags";
		public static final String CONSTANT_RATE_FACTOR = "-crf";
		public static final String SPEED_PRESET = "-preset";

		public static final String DISABLE_AUDIO = "-an";
		public static final String ENABLE_OUTFILE_OVERWRITE = "-y";
		public static final String SIZE = "-s";
		public static final String FRAMERATE = "-r";
		public static final String FORMAT = "-f";
		public static final String BITRATE_VIDEO = "-b:v";

		public static final String BITRATE_AUDIO = "-b:a";
		public static final String CHANNELS_AUDIO = "-ac";
		public static final String FREQ_AUDIO = "-ar";

		public static final String STARTTIME = "-ss";
		public static final String DURATION = "-t";
		public static final String LEVEL = "-level";
		public static final String BUFFER_SIZE = "-bufsize";
		public static final String STRICT = "-strict";
		public static final String HLS_OPT_TIME = "-hls_time";
		public static final String HLS_OPT_LIST_SIZE = "-hls_list_size";


	}

	public ArrayList<String> generateFFMPEGCommand(String targetResolution, File original, File outputParent){
		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);
		//cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);

		cmd.add(original.getName());

		cmd.add(Argument.AUDIOCODEC);
		cmd.add("aac");

		cmd.add(Argument.CHANNELS_AUDIO);
		cmd.add("2");

		cmd.add(Argument.AUDIO_BITRATE);
		cmd.add("64k");

		cmd.add(Argument.FREQ_AUDIO);
		cmd.add("44100");

		cmd.add(Argument.VIDEOCODEC);
		cmd.add("libx264");

		cmd.add(Argument.VIDEO_PROFILE);
		cmd.add("baseline");

		cmd.add(Argument.LEVEL);
		cmd.add("1.3");

		cmd.add(Argument.BUFFER_SIZE);
		cmd.add("1M");

		cmd.add(Argument.CONSTANT_RATE_FACTOR);
		cmd.add("18");

		cmd.add(Argument.FRAMERATE);
		cmd.add("25");

		cmd.add(Argument.KEYFRAME_INTERVAL);
		cmd.add("75");


		cmd.add(Argument.FORMAT);
		cmd.add("hls");

		cmd.add(Argument.HLS_OPT_LIST_SIZE);
		cmd.add("0");

		cmd.add(Argument.SIZE);
		cmd.add(targetResolution);

		cmd.add( outputParent.getAbsolutePath() + "/" + targetResolution + ".m3u8");

		return cmd;

	}

	public int execProcess(List<String> cmds, ShellCallback sc, File fileExec) throws IOException, InterruptedException {		

		//ensure that the arguments are in the correct Locale format
		for (String cmd :cmds)
		{
			cmd = String.format(Locale.US, "%s", cmd);
		}

		ProcessBuilder pb = new ProcessBuilder(cmds);
		
		pb.directory(fileExec);
		System.out.println("path to ffmpeg: " + mffmpegBin);
		System.out.println("file working directory: " + pb.directory().getAbsolutePath());

		StringBuffer cmdlog = new StringBuffer();

		for (String cmd : cmds)
		{
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}

		sc.shellOut(cmdlog.toString());

		Map<String, String> env = pb.environment();

		Process process = pb.start();    


		// any error message?
		StreamGobbler errorGobbler = new StreamGobbler(
				process.getErrorStream(), "ERROR", sc);

		// any output?
		StreamGobbler outputGobbler = new 
				StreamGobbler(process.getInputStream(), "OUTPUT", sc);

		errorGobbler.start();
		outputGobbler.start();

		int exitVal = process.waitFor();

		sc.processComplete(exitVal);

		return exitVal;

	}


	private class StreamGobbler extends Thread
	{
		InputStream is;
		String type;
		ShellCallback sc;

		StreamGobbler(InputStream is, String type, ShellCallback sc)
		{
			this.is = is;
			this.type = type;
			this.sc = sc;
		}

		public void run()
		{
			try
			{
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null)
					if (sc != null)
						sc.shellOut(line);

			} catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}


	public static int killVideoProcessor (boolean asRoot, boolean waitFor) throws IOException
	{
		int killDelayMs = 300;

		int result = -1;

		int procId = -1;

		while ((procId = ShellUtils.findProcessId(mffmpegBin)) != -1)
		{

			String[] cmd = { ShellUtils.SHELL_CMD_KILL + ' ' + procId + "" };

			try { 
				result = ShellUtils.doShellCommand(cmd, new StdoutCallback(), asRoot, waitFor);
				Thread.sleep(killDelayMs); }
			catch (Exception e){}
		}

		return result;
	}

	public ArrayList<String> generateFFMPEGBaselineCommand(File sourceFile,
			File output) {

		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);
		//cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);

		cmd.add(sourceFile.getName());

		cmd.add(Argument.VIDEOCODEC);
		cmd.add("libx264");

		cmd.add(Argument.AUDIOCODEC);
		cmd.add("aac");

		cmd.add(Argument.KEYFRAME_INTERVAL);
		cmd.add("90");

		cmd.add(Argument.CONSTANT_RATE_FACTOR);
		cmd.add("17");


		cmd.add(Argument.AUDIO_BITRATE);
		cmd.add("96k");

		//add output 
		cmd.add(output.getName());
		return cmd;
	}

	public ArrayList<String> generateFFMPEGFullSizeThumbnailCommand(String vidName, File sourceFile, File thumbnailParent) {
		//ffmpeg -ss 3 -i sample.mp4 -vf "select=gt(scene\,0.4)" -frames:v 5 -vsync vfr  out%02d.jpg

		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);

		cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);


		cmd.add(sourceFile.getName());

		cmd.add("-vf");
		cmd.add("fps=1/3");

		//add output 
		cmd.add(thumbnailParent.getName() + "/large/" + "thumbnail%02d.jpg");
		return cmd;
	}

	public ArrayList<String> generateFFMPEGMediumSizeThumbnailCommand(String vidName, File sourceFile, File thumbnailParent) {
		//ffmpeg -ss 3 -i sample.mp4 -vf "select=gt(scene\,0.4)" -frames:v 5 -vsync vfr  out%02d.jpg

		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);
		

		cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);


		cmd.add(sourceFile.getName());
		
		
		
		cmd.add("-vf");
		cmd.add("fps=1/3");
		cmd.add("-s");
		cmd.add("160x100");
		//add output 
		cmd.add(thumbnailParent.getName() +"/medium/"+ "thumbnail%02d.jpg");
		return cmd;
	}
	
	public ArrayList<String> generateFFMPEGSmallSizeThumbnailCommand(String vidName, File sourceFile, File thumbnailParent) {
		//ffmpeg -ss 3 -i sample.mp4 -vf "select=gt(scene\,0.4)" -frames:v 5 -vsync vfr  out%02d.jpg

		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);
		

		cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);


		cmd.add(sourceFile.getName());
		
		cmd.add("-vf");
		cmd.add("fps=1/3");
		cmd.add("-s");
		cmd.add("70x70");
		//add output 
		cmd.add(thumbnailParent.getName() +"/small/"+ "thumbnail%02d.jpg");
		return cmd;
	}


}

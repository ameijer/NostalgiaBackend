package com.nostalgia.contentserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FFMPEGController {

	//public static final String mffmpegBin = "bin/ffmpeg";
	public static final String mffmpegBin = "ffmpeg";

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
		cmd.add("libfdk_aac");
	
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

		StringBuffer cmdlog = new StringBuffer();

		for (String cmd : cmds)
		{
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}

		sc.shellOut(cmdlog.toString());

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
				//   Log.e(TAG,"error reading shell slog",ioe);
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
		cmd.add("libfdk_aac");

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

	public ArrayList<String> generateFFMPEGThumbnailCommand(String vidName, File sourceFile, File thumbnailParent) {
		
		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(mffmpegBin);
	
		cmd.add(Argument.ENABLE_OUTFILE_OVERWRITE);

		//add streaming input
		cmd.add(Argument.FILE_INPUT);
		

		cmd.add(sourceFile.getName());

		cmd.add("-vf");
		cmd.add("fps=1/3");

		//add output 
		cmd.add(thumbnailParent.getName() + "/" + vidName + "_thumbnail_%02d.jpg");
		return cmd;
	}


}

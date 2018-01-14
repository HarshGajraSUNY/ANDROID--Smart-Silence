package cs580.edu.binghamton.com.smartsilence;

import android.media.MediaRecorder;
import java.io.IOException;

public class SoundMeter
{
    public MediaRecorder mRecorder = null;

    public void start()
    {
        if (mRecorder == null)
        {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            //mRecorder.setAudioSamplingRate(48000); encoding format must be changed.
            //mRecorder.setAudioEncodingBitRate(384000);

            try
            {
                mRecorder.prepare();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            mRecorder.start();
        }
    }

    public void stop()
    {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude()
    {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 0;

    }
}

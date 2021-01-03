import sys
import datetime as dt

import librosa
import librosa.display
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

AUDIO_CODE = '[AUDIO_CODE]'


def plot_waveplot_regular(samples, sampling_rate, sec, title):
    # samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    samps = int(sampling_rate * sec)
    print(sampling_rate)

    plt.figure(figsize=(13.07, 7.35))
    librosa.display.waveplot(samples[0:samps], sr=sampling_rate, max_points=None, x_axis='s')
    plt.suptitle("Waveplot")
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Time (s)")
    plt.show()

    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.hist(samples, rwidth=0.95)
    plt.suptitle('Audio Value freq')
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.minorticks_on()
    plt.tight_layout()
    plt.show()


def plot_waveplot_detailed(samples, sampling_rate, sec, title):
    # samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    samps = int(sampling_rate * sec)

    time = np.linspace(0, len(samples) / sampling_rate, num=len(samples))

    plt.figure(figsize=(13.07, 7.35))
    plt.plot(time[0:samps], samples[0:samps])
    plt.suptitle("Waveplot")
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Time (s)")
    plt.show()


def plot_waveplot(samples, sampling_rate, sec, title):
    if sec < 0.5:
        plot_waveplot_detailed(samples, sampling_rate, sec, title)
    else:
        plot_waveplot_regular(samples, sampling_rate, sec, title)


def stft_plot(samples, sampling_rate, title):
    # samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    n = len(samples)
    hop_length = 1024 * 1
    print(sampling_rate)

    D = librosa.amplitude_to_db(np.abs(librosa.stft(samples, hop_length=hop_length)), ref=np.max)

    plt.figure(figsize=(13.07, 7.35))
    librosa.display.specshow(D, y_axis='log', sr=sampling_rate, hop_length=hop_length, x_axis='s')
    plt.colorbar(format='%+2.0f dB')
    plt.suptitle('Log-frequency power spectrogram')
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.show()


def histogram(filepath, title, data):
    if filepath is None and data is not None:
        data = data
    elif filepath is not None and data is None:
        data = pd.read_csv(filepath)
    else:
        print('Incorrect input!!!\n')

    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.hist(data, rwidth=0.95)
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.minorticks_on()
    plt.tight_layout()
    plt.show()


def AQ_DPCM_figures(file_path, audio_code):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 5, )
    # plot_waveplot(filepath + '.wav', 0.2)
    stft_plot(samples, sampling_rate, '[Title]')
    histogram(file_path + '_diffs' + '.csv', '[G13]', None)
    histogram(None, '[G14]', samples)
    histogram(file_path + '_mus' + '.csv', '[G15]', None)  # 'G17'
    histogram(file_path + '_betas' + '.csv', '[G16]', None)  # 'G18'


def DPCM_figures(file_path, audio_code):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 5, '[G10]')
    # plot_waveplot(filepath + '.wav', 0.2)
    stft_plot(samples, sampling_rate, '[Title]')
    histogram(file_path + '_diffs' + '.csv', '[G11]', None)
    histogram(None, '[G12]', samples)


def freq_figures(file_path, audio_code):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 5, '[G9]')
    # plot_waveplot(filepath + '.wav', 0.2)
    stft_plot(samples, sampling_rate, '[Title]')
    histogram(file_path + '_diffs' + '.csv', '[Title]', None)


if __name__ == '__main__':
    print('Main is NOT available')
    # plot_waveplot("./audioClient/_DPCM_track2_30s.wav", 5)
    # plot_waveplot("./audioClient/_DPCM_track2_30s.wav", 0.2)
    # stft_plot("./audioClient/_DPCM_track2_30s.wav")
    # histogram("./audioClient/_DPCM_track2_30s_diffs.csv")
    #
    # plot_waveplot("./audioClient/_AQ-DPCM_track2_10s.wav", 5)
    # stft_plot("./audioClient/_AQ-DPCM_track2_10s.wav")
    # histogram("./audioClient/_AQ-DPCM_track2_10s_diffs.csv")

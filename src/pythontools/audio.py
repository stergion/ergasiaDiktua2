import librosa
import librosa.display
import matplotlib.pyplot as plt
import numpy as np


def plot_waveplot_regular(file_path, sec):
    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    samps = (int)(sampling_rate * sec)
    print(sampling_rate)

    plt.figure(figsize=(13.07, 7.35))
    librosa.display.waveplot(samples[0:samps], sr=sampling_rate, max_points=None, x_axis='s')
    plt.title("Waveplot")
    plt.show()


def plot_waveplot_detailed(file_path, sec):
    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    samps = (int)(sampling_rate * sec)

    time = np.linspace(0, len(samples) / sampling_rate, num=len(samples))

    plt.figure(figsize=(13.07, 7.35))
    plt.plot(time[0:samps], samples[0:samps])
    plt.title("Waveplot")
    plt.xlabel("Time (s)")
    plt.show()


def plot_waveplot(file_path, sec):
    if (sec < 0.5):
        plot_waveplot_detailed(file_path, sec)
    else:
        plot_waveplot_regular(file_path, sec)


def stft_plot(file_path):
    samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    n = len(samples)
    hop_length = 1024 * 1
    print(sampling_rate)

    D = librosa.amplitude_to_db(np.abs(librosa.stft(samples, hop_length=hop_length)), ref=np.max)

    plt.figure(figsize=(13.07, 7.35))
    librosa.display.specshow(D, y_axis='log', sr=sampling_rate, hop_length=hop_length, x_axis='s')
    plt.colorbar(format='%+2.0f dB')
    plt.title('Log-frequency power spectrogram')
    plt.show()


if __name__ == '__main__':
    plot_waveplot("./audioClient/freqT30s.wav", 5)
    plot_waveplot("./audioClient/freqT30s.wav", 0.2)
    stft_plot("./audioClient/freqT30s.wav")

    plot_waveplot("./testAudioName.wav", 5)
    stft_plot("./testAudioName.wav")

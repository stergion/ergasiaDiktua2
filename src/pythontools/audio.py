import argparse
import datetime as dt
import os
import re

import librosa
import librosa.display
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

AUDIO_CODE = '[AUDIO_CODE]'
global dirPath


def get_proper_filename(string):
    delimiters = ".", " "

    regex_pattern = '|'.join(map(re.escape, delimiters))
    proper = re.sub(regex_pattern, "_", string)

    return proper


def plot_waveplot(samples, sampling_rate, sec, title):
    # samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    samps = int(sampling_rate * sec)

    plt.figure(figsize=(13.07, 7.35), dpi=200)

    if sec < 0.5:  # Detailed Waveplot
        time = np.linspace(0, len(samples) / sampling_rate, num=len(samples))
        plt.plot(time[0:samps], samples[0:samps])
    else:  # Regular Waveplot
        librosa.display.waveplot(samples[0:samps], sr=sampling_rate, max_points=None, x_axis='s')

    plt.suptitle(title)
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Time (s)", labelpad=20, fontsize=16)
    plt.ylabel("Amplitude", labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()


def specplot(samples, sampling_rate, title):
    # samples, sampling_rate = librosa.load(file_path, sr=None, mono=True, offset=0.0, duration=None)
    n = len(samples)
    hop_length = 1024 * 1
    print(sampling_rate)

    D = librosa.amplitude_to_db(np.abs(librosa.stft(samples, hop_length=hop_length)), ref=np.max)

    fig = plt.figure(figsize=(13.07, 7.35))
    librosa.display.specshow(D, y_axis='log', sr=sampling_rate, hop_length=hop_length, x_axis='s')
    plt.colorbar(format='%+2.0f dB')
    plt.suptitle(title)
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    fig_filename = get_proper_filename(title) + '.png'
    fig.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()


def histogram(filepath, title=None, data=None, xlabel=None, ylabel=None):
    if filepath is None and data is not None:
        data = data
    elif filepath is not None and data is None:
        data = pd.read_csv(filepath)
    else:
        print('histogram: Incorrect input!!!')
        print('filepath is None:', filepath is None)
        print('data is None:', data is None)

    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.hist(data, rwidth=0.95)
    plt.suptitle(title)
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel(xlabel, labelpad=20, fontsize=16)
    plt.ylabel(ylabel, labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()


def lineplot(filepath, title, xlabel=None, ylabel=None):
    data = pd.read_csv(filepath)

    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.plot(data)
    plt.suptitle(title)
    plt.title(label=AUDIO_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel(xlabel, labelpad=20, fontsize=16)
    plt.ylabel(ylabel, labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()


def get_file_name(string):
    delimiters = "\\", "/"

    regex_pattern = '|'.join(map(re.escape, delimiters))
    string_split = re.split(regex_pattern, string)

    return string_split[len(string_split) - 1]


def aq_dpcm_figures(audio_code, file_path1, file_path2):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    file_name1 = get_file_name(file_path1)
    file_name2 = get_file_name(file_path2)

    samples, sampling_rate = librosa.load(file_path1 + '.wav', sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 2, '[G10] Waveplot (' + file_name1 + '.wav)')
    specplot(samples, sampling_rate, 'Log-frequency power spectrogram (' + file_name1 + '.wav)')

    histogram(filepath=file_path1 + '_diffs' + '.csv',
              title='[G13] Frequency diagram of sound waveform differences (' + file_name1 + '.wav)',
              xlabel='Differences Values', ylabel='Frequency')
    histogram(None, title='[G14] Frequency diagram of sound waveform values (' + file_name1 + '.wav)', data=samples,
              xlabel='Sound Values', ylabel='Frequency')

    lineplot(file_path1 + '_mus' + '.csv', '[G15] Mean of Quantiser (' + file_name1 + '.wav)', xlabel='Packet')
    lineplot(file_path1 + '_betas' + '.csv', '[G16] Step of Quantiser (' + file_name1 + '.wav)', xlabel='Packet')
    lineplot(file_path2 + '_mus' + '.csv', '[G17] Mean of Quantiser (' + file_name2 + '.wav)', xlabel='Packet')
    lineplot(file_path2 + '_betas' + '.csv', '[G18] Step of Quantiser (' + file_name2 + '.wav)', xlabel='Packet')


def dpcm_figures(audio_code, file_path):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    file_name = get_file_name(file_path)

    samples, sampling_rate = librosa.load(file_path + '.wav', sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 2, '[G10] Waveplot (' + file_name + '.wav)')
    specplot(samples, sampling_rate, 'Log-frequency power spectrogram (' + file_name + '.wav)')

    histogram(filepath=file_path + '_diffs' + '.csv',
              title='[G11] Frequency diagram of sound waveform differences (' + file_name + '.wav)',
              xlabel='Differences Values', ylabel='Frequency')
    histogram(None, title='[G12] Frequency diagram of sound waveform values (' + file_name + '.wav)', data=samples,
              xlabel='Sound Values', ylabel='Frequency')


def freq_figures(audio_code, file_path):
    global AUDIO_CODE
    AUDIO_CODE = audio_code

    file_name = get_file_name(file_path)

    samples, sampling_rate = librosa.load(file_path + '.wav', sr=None, mono=True, offset=0.0, duration=None)

    plot_waveplot(samples, sampling_rate, 2, '[G9] Waveplot (' + file_name + '.wav)')
    specplot(samples, sampling_rate, 'Log-frequency power spectrogram (' + file_name + '.wav)')
    # histogram(file_path + '_diffs' + '.csv', '[Title]', None, None, None)


def bar(a1, a2):
    print(a1, a2)


def main(audio_type, audio_code, file_path1, file_path2=None, wdir: str = None):
    global dirPath

    if wdir is not None:
        wdir = wdir.replace('\\', os.sep)
        wdir = wdir.replace('/', os.sep)
        print("Current working directory: " + wdir)
        os.chdir(wdir)

    dirPath = os.path.join(os.getcwd(), 'figures', 'audio')
    if not os.path.exists(dirPath):
        os.makedirs(dirPath)
        # print(dirPath)
    if audio_type == 'AQ-DPCM':
        print('AQ-DPCM figures')
        file_path1 = file_path1.replace('\\', '/')
        file_path2 = file_path2.replace('\\', '/')
        aq_dpcm_figures(audio_code=audio_code, file_path1=file_path1, file_path2=file_path2)
    elif audio_type == 'DPCM':
        print('DPCM figures')
        file_path1 = file_path1.replace('\\', '/')
        dpcm_figures(audio_code=audio_code, file_path=file_path1)
    elif audio_type == 'freq':
        print('Frequency figures')
        file_path1 = file_path1.replace('\\', '/')
        freq_figures(audio_code=audio_code, file_path=file_path1)
    else:
        print('Incorrect input!')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("audio_type", help="audio type", type=str)
    parser.add_argument("audio_code", help="audio code", type=str)
    parser.add_argument("file_path1", help="file path 1", type=str)
    parser.add_argument("-fp2", "--file_path2", help="file path 2", type=str)
    parser.add_argument("-wdir", help="set working directory", type=str)
    args = parser.parse_args()

    main(args.audio_type, args.audio_code, args.file_path1, args.file_path2, args.wdir)

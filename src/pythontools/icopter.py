import argparse
import datetime as dt
import os
import re

import matplotlib.pyplot as plt
import pandas as pd

global ITHAKICOPTER_CODE
global dirPath


def get_proper_filename(string):
    delimiters = ".", " "

    regex_pattern = '|'.join(map(re.escape, delimiters))
    proper = re.sub(regex_pattern, "_", string)

    return proper


def plot(filepath, title=None, xlabel=None, ylabel=None):
    telemetry = pd.read_csv(filepath)
    pressure = telemetry['PRESSURE (mbar)']
    telemetry.drop(labels=['time', 'PRESSURE (mbar)'], axis=1, inplace=True)

    fig = plt.figure(num=1, figsize=(13.07, 7.35))
    ax = fig.subplots()
    telemetry.plot(ax=ax)
    pressure.plot(ax=ax, secondary_y=True, legend=True)
    plt.suptitle(title, y=0.98, fontsize=20)
    plt.title(label=ITHAKICOPTER_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    ax.set_xlabel(xlabel, labelpad=20, fontsize=16)
    ax.set_ylabel(ylabel, labelpad=20, fontsize=16)
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    fig.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()


def main(ithakicopter_code, file_path1, file_path2, wdir: str = None):
    global ITHAKICOPTER_CODE, dirPath
    ITHAKICOPTER_CODE = ithakicopter_code

    if wdir is not None:
        wdir = wdir.replace('\\', os.sep)
        wdir = wdir.replace('/', os.sep)
        print("Current working directory: " + wdir)
        os.chdir(wdir)

    dirPath = os.path.join(os.getcwd(), 'figures', 'ithakicopter')
    # print(dirPath)
    if not os.path.exists(dirPath):
        os.makedirs(dirPath)
        # print(dirPath)
    plot(file_path1, title='[G19] Ithakicopter Telemetry Data', xlabel='Time (s)')
    plot(file_path2, title='[G20] Ithakicopter Telemetry Data', xlabel='Time (s)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("ithakicopter_code", help="ithakicopter code", type=str)
    parser.add_argument("file_path1", help="file path 1", type=str)
    parser.add_argument("file_path2", help="file path 2", type=str)
    parser.add_argument("-wdir", help="set working directory", type=str)
    args = parser.parse_args()

    main(args.ithakicopter_code, args.file_path1, args.file_path2, args.wdir)

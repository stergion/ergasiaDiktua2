import argparse
import datetime as dt
import os
import re

import matplotlib.pyplot as plt
import pandas as pd

global OBD_CODE
global dirPath


def get_proper_filename(string):
    delimiters = ".", " "

    regex_pattern = '|'.join(map(re.escape, delimiters))
    proper = re.sub(regex_pattern, "_", string)

    return proper


def plot(filepath, title=None, xlabel=None, ylabel=None):
    telemetry = pd.read_csv(filepath)
    engine = telemetry[['ENGINE_TIME (sec)', 'ENGINE_RPM (RPM)']]
    telemetry.drop('ENGINE_RPM (RPM)', axis=1, inplace=True)
    col_names = telemetry.columns

    fig = plt.figure(num=1, figsize=(13.07, 7.35))
    ax = fig.subplots()
    if title is not None:
        plt.suptitle(title, y=0.98, fontsize=20)
    telemetry.plot(ax=ax, x='ENGINE_TIME (sec)', y=col_names[1:])
    engine.plot(ax=ax, x='ENGINE_TIME (sec)', secondary_y=True)
    plt.title(label=OBD_CODE + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    ax.set_xlabel(xlabel, labelpad=20, fontsize=16)
    ax.set_ylabel(ylabel, labelpad=20, fontsize=16)
    if xlabel is not None:
        ax.grid(which='major', axis='both', alpha=0.8)
    if ylabel is not None:
        ax.grid(which='minor', axis='x', alpha=0.2)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()


def main(obd_code, file_path, wdir: str = None):
    global dirPath, OBD_CODE
    OBD_CODE = obd_code

    if wdir is not None:
        wdir = wdir.replace('\\', os.sep)
        wdir = wdir.replace('/', os.sep)
        print("Current working directory: " + wdir)
        os.chdir(wdir)

    dirPath = os.path.join(os.getcwd(), 'figures', 'obd')
    if not os.path.exists(dirPath):
        os.makedirs(dirPath)
        # print(dirPath)

    plot(file_path, title='Vehicle OBD-II Telemetry Data')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("obd_code", help="obd code", type=str)
    parser.add_argument("file_path", help="file path", type=str)
    parser.add_argument("-wdir", help="set working directory", type=str)
    args = parser.parse_args()

    main(args.obd_code, args.file_path, args.wdir)

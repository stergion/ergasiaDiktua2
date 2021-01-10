import sys

import matplotlib.pyplot as plt
import pandas as pd


def plot(filepath, title=None, xlabel=None, ylabel=None):
    telemetry = pd.read_csv(filepath)

    plt.figure(num=1, figsize=(13.07, 5.35))
    print(telemetry.head())
    telemetry.plot(title=title, xlabel=xlabel, ylabel=ylabel)
    plt.show()


if __name__ == '__main__':
    fp1 = sys.argv[1]
    fp2 = sys.argv[2]

    plot(fp1, title='G19', xlabel='Time (s)')
    plot(fp2, title='G20', xlabel='Time (s)')

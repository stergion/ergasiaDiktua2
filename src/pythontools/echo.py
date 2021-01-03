import datetime as dt
import sys

import matplotlib.pyplot as plt
import pandas as pd

PACKET_SIZE = 32  # Packet size in bits

if __name__ == '__main__':
    directory = 'echoClient'

    echo_code = sys.argv[1]
    if echo_code == 'E0000':
        rt_plot_name = 'G1'
        rolling_plot_name = 'G2'
        rt_hist_name = 'G5'
        rolling_hist_name = 'G6'
        suptitle_comment = 'without delay'
        latencies = pd.read_csv("./echoClient/echoFast.csv")['latencies']
        times = pd.read_csv("./echoClient/echoFast.csv")['times']
    else:
        rt_plot_name = 'G3'
        rolling_plot_name = 'G4'
        rt_hist_name = 'G7'
        rolling_hist_name = 'G8'
        suptitle_comment = 'with delay'
        latencies = pd.read_csv("./echoClient/echoDelay.csv")['latencies']
        times = pd.read_csv("./echoClient/echoDelay.csv")['times']

    packets_per_sec = pd.Series(name='packets_per_sec', index=range(0, times.iloc[-1] // 1000 + 1), data=0)
    throughput = pd.Series(name='bps', index=range(0, times.iloc[-1] // 1000 + 1), data=0)

    for i in times:
        packets_per_sec.iloc[i // 1000] += 1  # packets received per second
    throughput = packets_per_sec.rmul(PACKET_SIZE)

    packets_per_sec_rolling_df = pd.DataFrame(data={'rolling8': throughput.rolling(8).mean()},
                                              index=range(len(throughput)))

    # Plots
    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.plot(latencies)
    plt.suptitle('[' + rt_plot_name + '] ' + 'Echo Response Time' + "(" + suptitle_comment + ")", fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Packet", labelpad=20, fontsize=16)
    plt.ylabel("RT (ms)", labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    plt.show()

    packets_per_sec_rolling_df.plot(figsize=(13.07, 5.35), legend=False)
    # plt.suptitle('Echo Response Time', fontsize=20)
    plt.gcf().suptitle('[' + rolling_plot_name + '] ' + "Throughput" + "(" + suptitle_comment + ")", y=0.98, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Time (s)", labelpad=20, fontsize=16)
    plt.ylabel("Throughput (bps)", labelpad=20, fontsize=16)
    plt.tight_layout()
    plt.show()

    # Histograms
    plt.figure(num=2, figsize=(13.07, 5.35))
    plt.hist(latencies, rwidth=0.95)
    plt.suptitle('[' + rt_hist_name + '] ' + 'Echo Frequencies of RTs' + "(" + suptitle_comment + ")", fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.ylabel("RT (ms)", labelpad=20, fontsize=16)
    plt.xlabel("Packets", labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    plt.show()

    packets_per_sec_rolling_df.hist(figsize=(13.07, 5.35), legend=False)
    # plt.suptitle('Echo Response Time', fontsize=20)
    plt.gcf().suptitle('[' + rolling_hist_name + '] ' + "Frequencies of Throughput Values" + "(" + suptitle_comment + ")", y=0.98, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Throughput (bps)", labelpad=20, fontsize=16)
    plt.ylabel("Frequency", labelpad=20, fontsize=16)
    plt.tight_layout()
    plt.show()

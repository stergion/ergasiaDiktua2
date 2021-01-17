import argparse
import datetime as dt
import os
import re

import matplotlib.pyplot as plt
import pandas as pd
import scipy.stats as st

PACKET_SIZE = 32.0  # Packet size in bits
global dirPath


def get_best_distribution(data, data_name):
    dist_names = ["norm", "exponweib", "weibull_max", "weibull_min", "pareto", "genextreme", "erlang", "expon", "gamma",
                  "lognorm", "maxwell"]
    dist_results = []
    dist_results_df = pd.DataFrame()
    params = {}
    for dist_name in dist_names:
        dist = getattr(st, dist_name)
        param = dist.fit(data)

        params[dist_name] = param
        # Applying the Kolmogorov-Smirnov test
        D, p = st.kstest(data, dist_name, args=param)
        print("p value for " + dist_name + " = " + str(p))
        dist_results.append((dist_name, p))
        dist_results_df = dist_results_df.append({'dist_name': dist_name,
                                                  'p': p}, ignore_index=True)

    # select the best fitted distribution
    print(dist_results_df.sort_values(by='p'))
    best_dist, best_p = (max(dist_results, key=lambda item: item[1]))
    # store the name of the best fit and its p value

    print(data_name)
    print("Best fitting distribution: " + str(best_dist))
    print("Best p value: " + str(best_p))
    print("Parameters for the best fit: " + str(params[best_dist]))

    # return best_dist, best_p, params[best_dist]


def get_proper_filename(string):
    delimiters = ".", " "

    regex_pattern = '|'.join(map(re.escape, delimiters))
    proper = re.sub(regex_pattern, "_", string)

    return proper


def main(echo_code, wdir: str = None):
    if wdir is not None:
        wdir = wdir.replace('\\', os.sep)
        wdir = wdir.replace('/', os.sep)
        print("Current working directory: " + wdir)
        os.chdir(wdir)

    global dirPath
    dirPath = os.path.join(os.getcwd(), 'figures', 'echo')
    if not os.path.exists(dirPath):
        os.makedirs(dirPath)
        # print(dirPath)

    if echo_code == 'E0000':
        rt_plot_name = 'G3'
        rolling_plot_name = 'G4'
        rt_hist_name = 'G7'
        rolling_hist_name = 'G8'
        suptitle_comment = 'without delay'
        latencies = pd.read_csv("./echoClient/echoFast.csv")['latencies']
        times = pd.read_csv("./echoClient/echoFast.csv")['times']
        rtt = None
    else:
        rt_plot_name = 'G1'
        rolling_plot_name = 'G2'
        rt_hist_name = 'G5'
        rolling_hist_name = 'G6'
        suptitle_comment = 'with delay'
        latencies = pd.read_csv("./echoClient/echoDelay.csv")['latencies']
        times = pd.read_csv("./echoClient/echoDelay.csv")['times']
        rtt = latencies
        rtt.name = 'RTT'

    packets_per_sec = pd.Series(name='packets_per_sec', index=range(0, times.iloc[-1] // 1000 + 1), data=0.0)

    print("*************************")
    print(echo_code)
    print("mean: " + str(latencies.mean()))
    print("std: " + str(latencies.std()))
    print("*************************")
    for i in times:
        packets_per_sec.iloc[i // 1000] += 1.0  # packets received per second
    throughput = packets_per_sec.rmul(PACKET_SIZE)

    packets_per_sec_rolling_df = pd.DataFrame(data={'rolling8': throughput.rolling(8).mean()},
                                              index=range(len(throughput)))

    # Plots
    plt.figure(num=1, figsize=(13.07, 5.35))
    plt.plot(latencies)
    title = '[' + rt_plot_name + '] ' + 'Echo Response Time' + "(" + suptitle_comment + ")"
    plt.suptitle(title, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Packet", labelpad=20, fontsize=16)
    plt.ylabel("RT (ms)", labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()

    packets_per_sec_rolling_df.plot(figsize=(13.07, 5.35), legend=False)
    title = '[' + rolling_plot_name + '] ' + "Throughput" + "(" + suptitle_comment + ")"
    plt.gcf().suptitle(title, y=0.98, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Time (s)", labelpad=20, fontsize=16)
    plt.ylabel("Throughput (bps)", labelpad=20, fontsize=16)
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()

    # Histograms
    plt.figure(num=2, figsize=(13.07, 5.35))
    plt.hist(latencies, rwidth=0.95)
    title = '[' + rt_hist_name + '] ' + 'Echo Frequencies of RTs' + "(" + suptitle_comment + ")"
    plt.suptitle(title, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("RT (ms)", labelpad=20, fontsize=16)
    plt.ylabel("Packets", labelpad=20, fontsize=16)
    plt.minorticks_on()
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # get_best_distribution(latencies, title)
    # plt.show()

    packets_per_sec_rolling_df.hist(figsize=(13.07, 5.35), legend=False)
    # plt.suptitle('Echo Response Time', fontsize=20)
    title = '[' + rolling_hist_name + '] ' + "Frequencies of Throughput Values" + "(" + suptitle_comment + ")"
    plt.gcf().suptitle(title, y=0.98, fontsize=20)
    plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
              fontweight='bold')
    plt.xlabel("Throughput (bps)", labelpad=20, fontsize=16)
    plt.ylabel("Frequency", labelpad=20, fontsize=16)
    plt.tight_layout()
    fig_filename = get_proper_filename(title) + '.png'
    plt.savefig(os.path.join(dirPath, fig_filename), format='png')
    plt.close()
    # plt.show()

    # R1 figure
    if rtt is not None:
        alpha = 0.875
        beta = 0.75
        gamma = 4

        srtt = pd.Series(index=range(len(rtt)), name='SRTT')
        sigma_rtt = pd.Series(index=range(len(rtt)), name='σ' +
                                                          '\N{LATIN SUBSCRIPT SMALL LETTER R}'
                                                          '\N{LATIN SUBSCRIPT SMALL LETTER T}'
                                                          '\N{LATIN SUBSCRIPT SMALL LETTER T}')
        # sigma_rtt = pd.Series(name='\N{GREEK CAPITAL LETTER SIGMA}r\N{SUBSCRIPT RTT}')
        rto = pd.Series(index=range(len(rtt)), name='RTO')

        srtt.iloc[0] = rtt.iloc[0]
        sigma_rtt.iloc[0] = abs(srtt.iloc[0] - rtt.iloc[0])
        rto.iloc[0] = srtt.iloc[0] + gamma * sigma_rtt.iloc[0]
        for i in range(1, len(rtt)):
            srtt.iloc[i] = alpha * srtt.iloc[i - 1] + (1 - alpha) * rtt.iloc[i]
            sigma_rtt[i] = beta * sigma_rtt.iloc[i - 1] + (1 - beta) * abs(srtt.iloc[i] - rtt.iloc[i])
            rto.iloc[i] = srtt.iloc[i] + gamma * sigma_rtt.iloc[i]

        plt.figure(num=3, figsize=(13.07, 5.35), dpi=250)
        rtt.plot()
        srtt.plot()
        sigma_rtt.plot()
        rto.plot()
        plt.legend(loc='best')
        title = '[R1]'
        plt.suptitle(title, y=0.98, fontsize=20)
        plt.title(label=echo_code + " - " + dt.datetime.now().isoformat(sep=' ', timespec='minutes'), fontsize=10,
                  fontweight='bold')
        plt.xlabel("Packet", labelpad=20, fontsize=16)
        plt.ylabel("Time (ms)", labelpad=20, fontsize=16)
        plt.tight_layout()
        fig_filename = get_proper_filename(title) + '.png'
        plt.savefig(os.path.join(dirPath, fig_filename), format='png')
        plt.close()
        # plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("echo_code", help="echo code", type=str)
    parser.add_argument("-wdir", help="set working directory", type=str)
    args = parser.parse_args()

    main(args.echo_code, args.wdir)

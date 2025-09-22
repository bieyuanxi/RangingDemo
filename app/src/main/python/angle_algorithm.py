import numpy as np
import pandas as pd


def sliding_window_mean(df, column, y=2):
    result = []
    # 遍历每一个整数角度
    for x in range(int(np.floor(df["angle_unwrap"].min())),
                   int(np.ceil(df["angle_unwrap"].max())) + 1):
        # 窗口范围
        low, high = x - y, x + y
        mask = (df["angle_unwrap"] >= low) & (df["angle_unwrap"] <= high)
        diff_mean = df.loc[mask, column].mean() if mask.any() else np.nan
        result.append({"angle": x, column + "_mean": diff_mean})
    return pd.DataFrame(result)



def calculate_angle(angle_list, diff_list):
    """
    :param angle_list: 角度列表
    :param diff_list: 差值列表
    :return: angle_at_min_diff：最小值所对应角度
    """
    column = 'diff'
    df = pd.DataFrame({"angle": angle_list, 'diff': diff_list})
    df["angle_unwrap"] = np.unwrap(np.deg2rad(df["angle"])) * 180 / np.pi
    df_sorted = df.sort_values(by="angle_unwrap", ascending=True)
    diff_df = sliding_window_mean(df_sorted, column, y=5)
    # 找到 diff 最小值所在的行
    min_row = diff_df.loc[diff_df[column + "_mean"].idxmin()]
    # 获取对应的 angle
    angle_at_min_diff = min_row["angle"]

    return angle_at_min_diff
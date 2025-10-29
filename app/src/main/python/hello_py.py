import numpy as np
import pandas as pd


def hello_python():
    return "Hello from Python!"


def np_version():
    return np.__version__


def pandas_version():
    return pd.__version__


def test_pandas():
    a = [1, 2, 3]
    b = [4, 5, 6]

    df = pd.DataFrame({'a': a, 'b': b})
    df['sum'] = df['a'] + df['b']
    tmp = df['sum'].to_list()
    return tmp
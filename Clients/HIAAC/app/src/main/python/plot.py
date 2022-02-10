from sklearn import manifold, datasets
import pandas as pd
import matplotlib.pylab as plt
from os.path import dirname, join
from umap import UMAP
import plotly.express as px
from com.chaquo.python import Python

n_components = 2 #@param {type:"slider", min:2, max:100, step:1}
metric = "correlation" #@param ["euclidean","manhattan","chebyshev","minkowski","canberra","braycurtis","haversine","mahalanobis","wminkowski","seuclidean","cosine","correlation","hamming","jaccard","dice","russellrao","kulsinski","rogerstanimoto","sokalmichener","sokalsneath","yule"]
n_neighbors = 41 #@param {type:"slider", min:5, max:200, step:1}
min_dist = 0.29 #@param {type:"slider", min:0, max:0.99, step:0.01}
umap_init = "random"

def plotPlotly(Y, labels, title):
    files_dir= str(Python.getPlatform().getApplication().getFilesDir())
    filename=join(dirname(files_dir),"plot.html")
    fig = px.scatter(Y, color=labels, labels={'color': 'category'})
    fig.update_layout(title_text=title, autosize=True)
    fig.update_xaxes(visible=False)
    fig.update_yaxes(visible=False)

    fig.write_html(filename)

def getX_Y(filename):
    filename = join(dirname(__file__), filename)
    data = pd.read_csv(filename, sep=';')

    acc=data.loc[data['Sensor Name'] == 'Accelerometer']
    acx=acc["Sensor Values"].str.split(",", expand = True)[0].str.split(":", expand = True)[1]
    acy=acc["Sensor Values"].str.split(",", expand = True)[1].str.split(":", expand = True)[1]
    acz=acc["Sensor Values"].str.split(",", expand = True)[2].str.split(":", expand = True)[1]

    g=data.loc[data['Sensor Name'] == 'Gyroscope']

    gx=g["Sensor Values"].str.split(",", expand = True)[0].str.split(":", expand = True)[1]
    gy=g["Sensor Values"].str.split(",", expand = True)[1].str.split(":", expand = True)[1]
    gz=g["Sensor Values"].str.split(",", expand = True)[2].str.split(":", expand = True)[1]

    X = pd.DataFrame()

    X["acx"]=acx[0:600].astype(float).values
    X["acy"]=acy[0:600].astype(float).values
    X["acz"]=acz[0:600].astype(float).values

    X["gx"]=gx[0:600].astype(float).values
    X["gy"]=gy[0:600].astype(float).values
    X["gz"]=gz[0:600].astype(float).values

    X["acx1"]=acx[0:600].astype(float).values
    X["acy1"]=acy[0:600].astype(float).values
    X["acz1"]=acz[0:600].astype(float).values

    X["gx1"]=gx[0:600].astype(float).values
    X["gy1"]=gy[0:600].astype(float).values
    X["gz1"]=gz[0:600].astype(float).values

    X["L"]=data["Label"]
    return X

def plotUmap(*filePaths):
    i = 0
    Xs = []
    for filePath in filePaths:
        Xs.append(getX_Y(filePath))
        i += 1

    X_ = pd.concat(Xs)

    X=X_.drop(columns=['L'])
    labels=X_["L"]

    umap_2d = UMAP()
    Y = umap_2d.fit_transform(X)
    fig = plt.figure(figsize=(15, 8))
    plotPlotly(Y, labels, "UMAP")

def plotTsne(*filePaths):
    i = 0
    Xs = []
    for filePath in filePaths:
        Xs.append(getX_Y(filePath))
        i += 1

    X_ = pd.concat(Xs)

    X=X_.drop(columns=['L'])
    labels=X_["L"]

    # n_points = 1000
    # X, color = datasets.make_s_curve(n_points, random_state=0)
    n_neighbors = 10
    n_components = 2
    # Create figure
    fig = plt.figure(figsize=(15, 8))
    fig.suptitle(
        "Manifold Learning with %i points, %i neighbors" % (1000, n_neighbors), fontsize=14
    )
    tsne= manifold.TSNE(n_components=n_components, init="pca", random_state=0)
    Y = tsne.fit_transform(X)
    plotPlotly(Y, labels, "TSNE")

def plotIsomap(*filePaths):
    i = 0
    Xs = []
    for filePath in filePaths:
        Xs.append(getX_Y(filePath))
        i += 1

    X_ = pd.concat(Xs)

    X=X_.drop(columns=['L'])
    labels=X_["L"]

    n_neighbors = 10
    n_components = 2
    fig = plt.figure(figsize=(15, 8))
    fig.suptitle(
        "Manifold Learning with %i points, %i neighbors" % (1000, n_neighbors), fontsize=14
    )

    isomap = manifold.Isomap(n_neighbors=n_neighbors, n_components=n_components)
    Y = isomap.fit_transform(X)
    plotPlotly(Y, labels, "ISOMAP")

def plotLle(*filePaths):
    return None

def plotDml(*filePaths):
    return None

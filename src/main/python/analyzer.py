from essentia.standard import MonoLoader, TensorflowPredictEffnetDiscogs, TensorflowPredict2D
import numpy as np
from os import listdir
from fastapi import FastAPI
api = FastAPI()


@api.get("/ping")
def ping():
    return {"status": "OK"}


@api.get("/analyze")
def analyze(audioPath: str):
    moodRoot = "./models/mood/"
    otherRoot = "./models/other/"
    models = listdir(moodRoot)

    invertedModels = ["mood_happy-discogs-effnet-1.pb",
                      "mood_aggressive-discogs-effnet-1.pb",
                      "danceability-discogs-effnet-1.pb",
                      "mood_electronic-discogs-effnet-1.pb"
                      ]
    audio = MonoLoader(sampleRate=16000, filename=audioPath)()
    ebModel = TensorflowPredictEffnetDiscogs(
        graphFilename="./models/discogs-effnet-bs64-1.pb",
        output="PartitionedCall:1")(audio)
    modelScores = {
        "scores": {
        },
        "instruments": [],
        "moods": [],
        "genres": []
    }
    for model in models:
        score = round(float(np.mean(TensorflowPredict2D(
            graphFilename=moodRoot+"/"+model,
            output="model/Softmax")(ebModel)[:, 1])),
            4)
        if model in invertedModels:
            score = round(1-score, 4)
        modelScores["scores"][model] = score

    predictions = np.mean(TensorflowPredict2D(graphFilename=otherRoot+"mtg_jamendo_instrument-discogs-effnet-1.pb",
                                              input="model/Placeholder",
                                              output="model/Sigmoid"
                                              )(ebModel), axis=0)
    for val in predictions:
        modelScores["instruments"].append(float(val))

    predictions = np.mean(TensorflowPredict2D(graphFilename=otherRoot+"mtg_jamendo_moodtheme-discogs-effnet-1.pb",
                                              input="model/Placeholder",
                                              output="model/Sigmoid"
                                              )(ebModel), axis=0)
    for val in predictions:
        modelScores["moods"].append(float(val))

    predictions = np.mean(TensorflowPredict2D(
        graphFilename=otherRoot+"genre_discogs400-discogs-effnet-1.pb",
        input="serving_default_model_Placeholder",
        output="PartitionedCall"
    )(ebModel), axis=0)
    for val in predictions:
        modelScores["genres"].append(float(val))

    return modelScores

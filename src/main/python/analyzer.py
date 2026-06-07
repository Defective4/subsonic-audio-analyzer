from essentia.standard import MonoLoader, TensorflowPredictEffnetDiscogs, TensorflowPredict2D
import numpy as np
from os import listdir
from fastapi import FastAPI
api = FastAPI()


@api.get("/analyze")
def analyze(audioPath: str):
    modelRoot = "./models"
    models = listdir(modelRoot)
    models.remove("discogs-effnet-bs64-1.pb")
    invertedModels = ["mood_happy-discogs-effnet-1.pb",
                      "mood_aggressive-discogs-effnet-1.pb",
                      "danceability-discogs-effnet-1.pb",
                      "mood_electronic-discogs-effnet-1.pb"
                      ]
    audio = MonoLoader(sampleRate=16000, resampleQuality=4, filename=audioPath)()
    ebModel = TensorflowPredictEffnetDiscogs(
        graphFilename="./models/discogs-effnet-bs64-1.pb",
        output="PartitionedCall:1")(audio)
    modelScores = {
    }
    for model in models:
        score = round(float(np.mean(TensorflowPredict2D(
            graphFilename=modelRoot+"/"+model,
            output="model/Softmax")(ebModel)[:, 1])),
            4)
        if model in invertedModels:
            score = round(1-score, 4)
        modelScores[model] = score
    return modelScores

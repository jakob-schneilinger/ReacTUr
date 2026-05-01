from fastapi import FastAPI

app = FastAPI()

@app.get("/api/python-ping")
def ping():
    return {"status": "Python RDKit Service ist hier!"}
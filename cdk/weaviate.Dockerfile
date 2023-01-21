FROM semitechnologies/weaviate:1.17.0

EXPOSE 80

CMD [ "--host", "0.0.0.0", "--port", "80", "--scheme", "http"]
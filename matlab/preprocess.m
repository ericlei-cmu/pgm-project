load('../Data/IMC/GeneFeatures.mat')
load('../Data/IMC/genes_phenes.mat')
load('../Data/IMC/clinicalfeatures_tfidf.mat')

f_summary = fopen('../Data/IMC/info','w');
fprintf(f_summary, '%d genes\n', numGenes);
fprintf(f_summary, '%d diseases\n', numPhenes);
fprintf(f_summary, '%d gene_features\n', size(GeneFeatures,2));
fprintf(f_summary, '%d disease_features\n', size(F,2));
fclose(f_summary);

gene_features = bsxfun(@rdivide, GeneFeatures, max(GeneFeatures) - min(GeneFeatures));
gene_features = bsxfun(@minus, gene_features, min(gene_features));
csvwrite('../Data/IMC/gene_features.csv',gene_features)

csvwrite('../Data/IMC/disease_features.csv',F)

[gene, phene, data] = find(GenePhene{1});
data = [gene,phene,data];
csvwrite('../Data/IMC/data.csv',data);

[gene1, gene2, data] = find(GeneGene_Hs);
data = [gene1,gene2,data];
csvwrite('../Data/IMC/gene_gene.csv',data);

% PhenotypeSimilaritiesLog(PhenotypeSimilaritiesLog < 0.0001) = 0;
% csvwrite('../Data/IMC/disease_disease.csv', PhenotypeSimilaritiesLog);

[disease1, disease2, data] = find(PhenotypeSimilarities);
data = [disease1,disease2,data];
csvwrite('../Data/IMC/disease_disease.csv',data);

    
# Fontes do catálogo de problemas odontológicos (V2__catalogo_problemas.sql)

Data da pesquisa: 15/11/2026. Arquivo gerado: `src/main/resources/db/migration/V2__catalogo_problemas.sql` (75 itens).

> **Limitação de rede.** Neste ambiente só o GitHub estava acessível. `www2.datasus.gov.br`, `icd.who.int`,
> `website.cfo.org.br`, `www.planalto.gov.br` e Wikipedia deram erro de DNS. Por isso:
> - os códigos CID-10 foram conferidos em **cópias dos arquivos oficiais** (CSV do DATASUS e dados da CID-10 da OMS
>   versão 2019), hospedadas no GitHub (links abaixo);
> - a lista do CFO e os textos normativos foram conferidos em fontes secundárias. Tudo o que não foi possível
>   ver na fonte primária está indicado como **(não verificado na fonte primária)**.

## (a) URLs consultadas

### CID-10
| URL | Situação |
|---|---|
| http://www2.datasus.gov.br/cid10/V2008/WebHelp/k00_k14.htm (e s00_s09, b00_b09, b35_b49, r10_r19, r50_r69, z00_z13) | inacessível (DNS) |
| https://icd.who.int/browse10/2019/en | inacessível (DNS) |
| https://raw.githubusercontent.com/cleytonferrari/CidDataSus/HEAD/CIDImport/Repositorio/Resources/CID-10-SUBCATEGORIAS.CSV | **consultado**: cópia do `CID-10-SUBCATEGORIAS.CSV` (pacote CID10CSV do DATASUS, versão 2008, ISO-8859-1). Usado para validar automaticamente **todos** os códigos do SQL |
| https://raw.githubusercontent.com/cleytonferrari/CidDataSus/HEAD/CIDImport/Repositorio/Resources/CID-10-CATEGORIAS.CSV | **consultado** (categorias de 3 caracteres) |
| https://github.com/kachamaka/icd10 (pasta `icd10_data/`, um JSON por subcategoria, extraído do browser OMS ICD-10 2019) | **consultado**: títulos em inglês, termos de inclusão e exclusão (ex.: K08.8 "Toothache NOS", K03.8 "Sensitive dentine", F45.8 "Teeth-grinding", K05.2 "Acute pericoronitis") |

### CFO / legislação
| URL | Situação |
|---|---|
| https://website.cfo.org.br/ | inacessível (DNS) |
| https://github.com/lexifina/brazil-law/blob/main/lei-13787-de-2018/art-006.md | **consultado**: texto do art. 6º da Lei 13.787/2018 |
| https://github.com/Licitei/leis/blob/main/federal/br-federal-lei-2018-12-27-13787.md | **consultado**: texto integral da Lei 13.787/2018 |
| https://github.com/martinezsalmeron/dentalpin-web (blog pt-br: `prontuario-eletronico-odontologico.md`, `cobrar-tratamento-odontologico-nao-pago.md`, `atender-adolescente-desacompanhado-odontologia.md`, `suspeita-maus-tratos-consultorio-odontologico.md`) | **consultado** (secundária, cita Res. CFO-91/2009, CFO-118/2012, CFO-271/2025, Manual do Prontuário do CFO de 2026) |
| https://github.com/raelvvieira/nosconecta/blob/main/src/lib/settings/especialidades.ts e https://github.com/rhaymisonbetini/faker-brasil/blob/master/src/helpers/HealthHelpers.js | **consultado** (secundária: listas das especialidades reconhecidas pelo CFO) |
| Referências primárias citadas pelas fontes secundárias, a abrir quando houver rede: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13787.htm · https://transparencia.cfo.org.br/ato-normativo/?id=1360 (Res. CFO-91/2009) · https://website.cfo.org.br/wp-content/uploads/2018/03/codigo_etica.pdf (Código de Ética, Res. CFO-118/2012) · https://website.cfo.org.br/wp-content/uploads/2026/03/CFO_Manual_do_Prontuario_Ebook_v2.pdf (Manual do Prontuário, 2026) · https://website.cfo.org.br/wp-content/uploads/2026/03/FAQ-PERGUNTAS-FREQUENTES-1.pdf · https://website.cfo.org.br/wp-content/uploads/2020/04/Consolidacao.pdf (Consolidação, Res. CFO-63/2005) | não abertas (DNS) |

## (b) Código interno → CID-10 → descrição oficial confirmada

Todos os 75 códigos existem no CSV oficial do DATASUS (checagem automática no script gerador). A coluna OMS mostra o título
da CID-10 2019 e os termos de inclusão que justificam a escolha.

| # | código interno | prioridade | especialidade | CID-10 | descrição oficial DATASUS (pt-BR) | título OMS 2019 (inclusões relevantes) |
|---|---|---|---|---|---|---|
| 1 | CELULITE_ABSCESSO_DISSEMINADO | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | K12.2 | Celulite e abscesso da boca | Cellulitis and abscess of mouth; inclui: Cellulitis of mouth (floor), Submandibular abscess |
| 2 | SANGRAMENTO_POS_EXTRACAO | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | T81.0 | Hemorragia e hematoma complicando procedimento não classificado em outra parte | Haemorrhage and haematoma complicating a procedure, not elsewhere classified; inclui: Haemorrhage at any site resulting from a procedure |
| 3 | AVULSAO_DENTARIA | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | S03.2 | Luxação dentária | Dislocation of tooth |
| 4 | LUXACAO_DENTARIA | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | S03.2 | Luxação dentária | Dislocation of tooth |
| 5 | TRAUMA_BUCAL_RECENTE | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | S01.5 | Ferimento do lábio e da cavidade oral | — |
| 6 | FRATURA_MANDIBULA | EMERGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | S02.6 | Fratura de mandíbula | Fracture of mandible; inclui: Lower jaw (bone) |
| 7 | DOR_DE_DENTE | URGENCIA | Clínica Geral | K08.8 | Outros transtornos especificados dos dentes e das estruturas de sustentação | Other specified disorders of teeth and supporting structures; inclui: Alveolar (process) cleft, Enlargement of alveolar ridge NOS, Irregular alveolar process, Toothache NOS |
| 8 | PULPITE | URGENCIA | Endodontia | K04.0 | Pulpite | Pulpitis; inclui: Pulpitis: NOS, Pulpitis: acute, Pulpitis: chronic (hyperplastic)(ulcerative), Pulpitis: irreversible |
| 9 | NECROSE_PULPAR | ROTINA | Endodontia | K04.1 | Necrose da polpa | Necrosis of pulp; inclui: Pulpal gangrene |
| 10 | TRATAMENTO_CANAL | ROTINA | Endodontia | K04.9 | Outras doenças da polpa e dos tecidos periapicais e as não especificadas | Other and unspecified diseases of pulp and periapical tissues |
| 11 | PERIODONTITE_APICAL_AGUDA | URGENCIA | Endodontia | K04.4 | Periodontite apical aguda de origem pulpar | Acute apical periodontitis of pulpal origin; inclui: Acute apical periodontitis NOS |
| 12 | ABSCESSO_COM_FISTULA | URGENCIA | Endodontia | K04.6 | Abscesso periapical com fístula | Periapical abscess with sinus; inclui: Dental abscess with sinus, Dentoalveolar abscess with sinus |
| 13 | ABSCESSO_SEM_FISTULA | URGENCIA | Endodontia | K04.7 | Abscesso periapical sem fístula | Periapical abscess without sinus; inclui: Dental abscess NOS, Dentoalveolar abscess NOS, Periapical abscess NOS |
| 14 | CARIE | ROTINA | Dentística | K02.1 | Cáries da dentina | Caries of dentine |
| 15 | RESTAURACAO_QUEBRADA | ROTINA | Dentística | K08.8 | Outros transtornos especificados dos dentes e das estruturas de sustentação | Other specified disorders of teeth and supporting structures; inclui: Alveolar (process) cleft, Enlargement of alveolar ridge NOS, Irregular alveolar process, Toothache NOS |
| 16 | FRATURA_DENTAL | URGENCIA | Dentística | S02.5 | Fratura de dentes | Fracture of tooth; inclui: Broken tooth |
| 17 | HIPERSENSIBILIDADE_DENTINARIA | ROTINA | Dentística | K03.8 | Outras doenças especificadas dos tecidos duros dos dentes | Other specified diseases of hard tissues of teeth; inclui: Irradiated enamel, Sensitive dentine |
| 18 | EROSAO_DENTAL | ROTINA | Dentística | K03.2 | Erosão dentária | Erosion of teeth; inclui: Erosion of teeth: NOS, Erosion of teeth: due to: diet, Erosion of teeth: due to: drugs and medicaments, Erosion of teeth: due to: persistent vomiting |
| 19 | ABRASAO_DENTAL | ROTINA | Dentística | K03.1 | Abrasão dentária | Abrasion of teeth; inclui: Abrasion: dentifrice of teeth, Abrasion: habitual of teeth, Abrasion: occupational of teeth, Abrasion: ritual of teeth |
| 20 | DESGASTE_DENTAL | ROTINA | Dentística | K03.0 | Atrito dentário excessivo | Excessive attrition of teeth; inclui: Wear: approximal of teeth, Wear: occlusal of teeth |
| 21 | MANCHAS_DENTES | ROTINA | Dentística | K03.7 | Alterações pós-eruptivas da cor dos tecidos duros dos dentes | Posteruptive colour changes of dental hard tissues |
| 22 | FLUOROSE | ROTINA | Dentística | K00.3 | Dentes manchados | Mottled teeth; inclui: Dental fluorosis, Mottling of enamel, Nonfluoride enamel opacities |
| 23 | CLAREAMENTO | ROTINA | Dentística | Z41.8 | Outros procedimentos para outros propósitos exceto cuidados de saúde | Other procedures for purposes other than remedying health state |
| 24 | ESTETICA_SORRISO | ROTINA | Dentística | Z41.8 | Outros procedimentos para outros propósitos exceto cuidados de saúde | Other procedures for purposes other than remedying health state |
| 25 | GENGIVITE | ROTINA | Periodontia | K05.1 | Gengivite crônica | Chronic gingivitis; inclui: Gingivitis (chronic): NOS, Gingivitis (chronic): desquamative, Gingivitis (chronic): hyperplastic, Gingivitis (chronic): simple marginal |
| 26 | SANGRAMENTO_GENGIVAL | ROTINA | Periodontia | K05.1 | Gengivite crônica | Chronic gingivitis; inclui: Gingivitis (chronic): NOS, Gingivitis (chronic): desquamative, Gingivitis (chronic): hyperplastic, Gingivitis (chronic): simple marginal |
| 27 | PERIODONTITE | ROTINA | Periodontia | K05.3 | Periodontite crônica | Chronic periodontitis; inclui: Chronic pericoronitis, Periodontitis: NOS, Periodontitis: complex, Periodontitis: simplex |
| 28 | ABSCESSO_PERIODONTAL | URGENCIA | Periodontia | K05.2 | Periodontite aguda | Acute periodontitis; inclui: Acute pericoronitis, Parodontal abscess, Periodontal abscess |
| 29 | RETRACAO_GENGIVAL | ROTINA | Periodontia | K06.0 | Retração gengival | Gingival recession; inclui: Gingival recession (generalized)(localized)(postinfective)(post-operative) |
| 30 | MOBILIDADE_DENTARIA | ROTINA | Periodontia | K08.8 | Outros transtornos especificados dos dentes e das estruturas de sustentação | Other specified disorders of teeth and supporting structures; inclui: Alveolar (process) cleft, Enlargement of alveolar ridge NOS, Irregular alveolar process, Toothache NOS |
| 31 | TARTARO | ROTINA | Periodontia | K03.6 | Depósitos nos dentes | Deposits [accretions] on teeth; inclui: Dental calculus: subgingival, Dental calculus: supragingival, Deposits [accretions] on teeth: betel, Deposits [accretions] on teeth: black |
| 32 | TERCEIRO_MOLAR | ROTINA | Cirurgia e Traumatologia Bucomaxilofaciais | K01.1 | Dentes impactados | Impacted teeth |
| 33 | PERICORONARITE | URGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | K05.2 | Periodontite aguda | Acute periodontitis; inclui: Acute pericoronitis, Parodontal abscess, Periodontal abscess |
| 34 | DENTE_INCLUSO | ROTINA | Cirurgia e Traumatologia Bucomaxilofaciais | K01.0 | Dentes inclusos | Embedded teeth |
| 35 | EXTRACAO_DENTARIA | ROTINA | Cirurgia e Traumatologia Bucomaxilofaciais | K08.3 | Raiz dentária retida | Retained dental root |
| 36 | ALVEOLITE | URGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | K10.3 | Alveolite maxilar | Alveolitis of jaws; inclui: Alveolar osteitis, Dry socket |
| 37 | DOR_INCHACO_POS_OPERATORIO | URGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | T81.8 | Outras complicações de procedimentos não classificadas em outra parte | Other complications of procedures, not elsewhere classified; inclui: Complication of inhalation therapy, Emphysema (subcutaneous) resulting from a procedure, Persistent postoperative fistula |
| 38 | CISTO_MAXILARES | ROTINA | Cirurgia e Traumatologia Bucomaxilofaciais | K09.2 | Outros cistos das mandíbulas | Other cysts of jaw; inclui: lesions showing histological features both of aneurysmal cyst and of another fibro-osseous lesion, Cyst of jaw: NOS, Cyst of jaw: aneurysmal, Cyst of jaw: haemorrhagic |
| 39 | SIALOLITIASE | ROTINA | Cirurgia e Traumatologia Bucomaxilofaciais | K11.5 | Sialolitíase | Sialolithiasis; inclui: Calculus of salivary gland or duct, Stone of salivary gland or duct |
| 40 | BRUXISMO | ROTINA | Disfunção Temporomandibular e Dor Orofacial | F45.8 | Outros transtornos somatoformes | Other somatoform disorders; inclui: disorders of psychological development, Psychogenic: dysmenorrhoea, Psychogenic: dysphagia, including "globus hystericus", Psychogenic: pruritus |
| 41 | DTM | ROTINA | Disfunção Temporomandibular e Dor Orofacial | K07.6 | Transtornos da articulação temporomandibular | Temporomandibular joint disorders; inclui: Costen complex or syndrome, Derangement of temporomandibular joint, Snapping jaw, Temporomandibular joint-pain-dysfunction syndrome |
| 42 | LUXACAO_MANDIBULA | URGENCIA | Cirurgia e Traumatologia Bucomaxilofaciais | S03.0 | Luxação do maxilar | Dislocation of jaw; inclui: Jaw (cartilage)(meniscus), Mandible, Temporomandibular (joint) |
| 43 | AFTAS | ROTINA | Estomatologia | K12.0 | Aftas bucais recidivantes | Recurrent oral aphthae; inclui: Aphthous stomatitis (major)(minor), Bednar aphthae, Periadenitis mucosa necrotica recurrens, Recurrent aphthous ulcer |
| 44 | HERPES_LABIAL | ROTINA | Estomatologia | B00.1 | Dermatite vesicular devido ao vírus do herpes | Herpesviral vesicular dermatitis; inclui: Herpes simplex: facialis, Herpes simplex: labialis, Vesicular dermatitis of: ear due to human (alpha) herpesvirus 2, Vesicular dermatitis of: lip due to human (alpha) herpesvirus 2 |
| 45 | CANDIDIASE | ROTINA | Estomatologia | B37.0 | Estomatite por Candida | Candidal stomatitis; inclui: candidosis, moniliasis, Oral thrush |
| 46 | ESTOMATITE_PROTESE | ROTINA | Prótese Dentária | K12.1 | Outras formas de estomatite | Other forms of stomatitis; inclui: Stomatitis: NOS, Stomatitis: denture, Stomatitis: ulcerative, Stomatitis: vesicular |
| 47 | LESAO_NAO_CICATRIZA | URGENCIA | Estomatologia | K13.7 | Outras lesões e as não especificadas da mucosa oral | Other and unspecified lesions of oral mucosa; inclui: Focal oral mucinosis |
| 48 | LEUCOPLASIA | URGENCIA | Estomatologia | K13.2 | Leucoplasia e outras afecções do epitélio oral, inclusive da língua | Leukoplakia and other disturbances of oral epithelium, including tongue; inclui: Erythroplakia of oral epithelium, including tongue, Leukoedema of oral epithelium, including tongue, Leukokeratosis nicotina palati, Smoker palate |
| 49 | MUCOCELE | ROTINA | Estomatologia | K11.6 | Mucocele de glândula salivar | Mucocele of salivary gland; inclui: Mucous: extravasation cyst of salivary gland, Mucous: retention cyst of salivary gland, Ranula |
| 50 | XEROSTOMIA | ROTINA | Estomatologia | R68.2 | Boca seca, não especificada | Dry mouth, unspecified |
| 51 | HALITOSE | ROTINA | Clínica Geral | R19.6 | Halitose | Halitosis |
| 52 | COROA_PROTESE_FIXA | ROTINA | Prótese Dentária | Z46.3 | Colocação e ajustamento de dispositivo de prótese dentária | Fitting and adjustment of dental prosthetic device |
| 53 | PROTESE_REMOVIVEL | ROTINA | Prótese Dentária | Z46.3 | Colocação e ajustamento de dispositivo de prótese dentária | Fitting and adjustment of dental prosthetic device |
| 54 | PERDA_DENTARIA | ROTINA | Prótese Dentária | K08.1 | Perda de dentes devida a acidente, extração ou a doenças periodontais localizadas | Loss of teeth due to accident, extraction or local periodontal disease |
| 55 | IMPLANTE_DENTARIO | ROTINA | Implantodontia | K08.1 | Perda de dentes devida a acidente, extração ou a doenças periodontais localizadas | Loss of teeth due to accident, extraction or local periodontal disease |
| 56 | PERI_IMPLANTITE | URGENCIA | Implantodontia | T85.7 | Infecção e reação inflamatória devida a outros dispositivos protéticos, implantes e enxertos internos | Infection and inflammatory reaction due to other internal prosthetic devices, implants and grafts |
| 57 | PROBLEMA_IMPLANTE | ROTINA | Implantodontia | T85.6 | Complicação mecânica de outros dispositivos protéticos, implantes e enxertos internos especificados | Mechanical complication of other specified internal prosthetic devices, implants and grafts; inclui: Conditions listed in T82.0 due to: epidural and subdural infusion catheter, Conditions listed in T82.0 due to: intraperitoneal dialysis catheter, Conditions listed in T82.0 due to: nonabsorbable surgical material NOS, Conditions listed in T82.0 due to: permanent sutures |
| 58 | DENTES_TORTOS | ROTINA | Ortodontia | K07.3 | Anomalias da posição dos dentes | Anomalies of tooth position; inclui: Crowding of tooth or teeth, Diastema of tooth or teeth, Displacement of tooth or teeth, Rotation of tooth or teeth |
| 59 | MA_OCLUSAO | ROTINA | Ortodontia | K07.2 | Anomalias da relação entre as arcadas dentárias | Anomalies of dental arch relationship; inclui: Crossbite (anterior)(posterior), Disto-occlusion, Mesio-occlusion, Midline deviation of dental arch |
| 60 | AVALIACAO_ORTODONTICA | ROTINA | Ortodontia | Z46.4 | Colocação e ajustamento de aparelho ortodôntico | Fitting and adjustment of orthodontic device |
| 61 | APARELHO_QUEBRADO | ROTINA | Ortodontia | Z46.4 | Colocação e ajustamento de aparelho ortodôntico | Fitting and adjustment of orthodontic device |
| 62 | HABITOS_ORTOPEDIA_FUNCIONAL | ROTINA | Ortopedia Funcional dos Maxilares | K07.5 | Anormalidades dentofaciais funcionais | Dentofacial functional abnormalities; inclui: Abnormal jaw closure, Malocclusion due to: abnormal swallowing, Malocclusion due to: mouth breathing, Malocclusion due to: tongue, lip or finger habits |
| 63 | ERUPCAO_DENTARIA | ROTINA | Odontopediatria | K00.7 | Síndrome da erupção dentária | Teething syndrome |
| 64 | CARIE_INFANTIL | ROTINA | Odontopediatria | K02.9 | Cárie dentária, sem outra especificação | Dental caries, unspecified |
| 65 | DENTE_LEITE_NAO_CAI | ROTINA | Odontopediatria | K00.6 | Distúrbios da erupção dentária | Disturbances in tooth eruption; inclui: Dentia praecox, Natal tooth, Neonatal tooth, Premature: eruption of tooth |
| 66 | AGENESIA_DENTARIA | ROTINA | Odontopediatria | K00.0 | Anodontia | Anodontia; inclui: Hypodontia, Oligodontia |
| 67 | DENTE_SUPRANUMERARIO | ROTINA | Odontopediatria | K00.1 | Dentes supranumerários | Supernumerary teeth; inclui: Distomolar, Fourth molar, Mesiodens, Paramolar |
| 68 | ANOMALIA_FORMA_DENTE | ROTINA | Odontopediatria | K00.2 | Anomalias do tamanho e da forma dos dentes | Abnormalities of size and form of teeth; inclui: Concrescence of teeth, Fusion of teeth, Gemination of teeth, Dens: evaginatus |
| 69 | DEFEITO_ESMALTE | ROTINA | Odontopediatria | K00.4 | Distúrbios na formação dos dentes | Disturbances in tooth formation; inclui: Aplasia and hypoplasia of cementum, Dilaceration of tooth, Enamel hypoplasia (neonatal)(postnatal)(prenatal), Regional odontodysplasia |
| 70 | LINGUA_PRESA | ROTINA | Odontopediatria | Q38.1 | Anquiloglossia | Ankyloglossia; inclui: Tongue tie |
| 71 | CHECKUP | ROTINA | Clínica Geral | Z01.2 | Exame dentário | Dental examination; inclui: routine examination of specific system |
| 72 | LIMPEZA_PROFILAXIA | ROTINA | Clínica Geral | Z29.8 | Outras medidas profiláticas especificadas | Other specified prophylactic measures |
| 73 | APLICACAO_FLUOR | ROTINA | Clínica Geral | Z29.8 | Outras medidas profiláticas especificadas | Other specified prophylactic measures |
| 74 | ORCAMENTO | ROTINA | Clínica Geral | Z01.2 | Exame dentário | Dental examination; inclui: routine examination of specific system |
| 75 | AVALIACAO_GERAL | ROTINA | Clínica Geral | Z01.2 | Exame dentário | Dental examination; inclui: routine examination of specific system |

### Observações sobre as escolhas
- **Bruxismo**: a CID-10 não tem código próprio. "Teeth-grinding" está incluído em **F45.8** (confirmado na OMS). G47.8 não traz bruxismo; o bruxismo do sono (G47.63) só existe na CID-10-CM americana.
- **Hipersensibilidade dentinária**: **K03.8** inclui "Sensitive dentine" (confirmado).
- **Pericoronarite**: K05.2 inclui "Acute pericoronitis" e K05.3 "Chronic pericoronitis" (confirmado). Não existe K05.22 na CID-10 OMS/DATASUS; esse código é da CID-10-CM.
- **Dor de dente**: K08.8 inclui "Toothache NOS" (confirmado).
- **Peri-implantite e problemas mecânicos de implante**: sem código específico na CID-10 OMS/DATASUS. Usados **T85.7** (infecção e reação inflamatória devida a outros dispositivos protéticos, implantes e enxertos internos) e **T85.6** (complicação mecânica). Há comentário na linha do SQL. A existência de Z96.5 ("Presença de raiz dentária e implantes mandibulares") mostra que a CID trata o implante dentário como implante interno.
- **Restauração quebrada e mobilidade dentária**: sem código específico (K08.5x só existe na CID-10-CM). Usado K08.8, com comentário no SQL.
- **Boca seca**: R68.2. A OMS exclui de R68.2 a hipossecreção salivar (K11.7), e K11.7 exclui "dry mouth NOS". Por isso R68.2 é o código certo para a queixa, e K11.7 só serve depois de confirmada a hipossalivação.
- **Avulsão**: classificada como luxação dentária, S03.2 (o título está confirmado; a inclusão de "avulsão" segue o índice alfabético da CID e não foi vista diretamente).
- **Siso**: K01.1 (impactado; serve para semi-incluso). Dente totalmente incluso sem obstrução = K01.0 (usado em DENTE_INCLUSO).
- **Celulite/abscesso disseminado**: K12.2 inclui "Cellulitis of mouth (floor)" e "Submandibular abscess". Celulite só da face = L03.2.
- **K02.5 (cárie com exposição pulpar)** e **K12.3 (mucosite oral)** existem na OMS 2019 mas **não** no CSV do DATASUS 2008, então não foram usados.
- **Procedimentos sem doença**: Z01.2 = exame dentário (confirmado, usado em AVALIACAO_GERAL, CHECKUP e ORCAMENTO). Z29.8 = outras medidas profiláticas (limpeza e flúor). Z41.8 = procedimentos para outros propósitos exceto cuidados de saúde (clareamento e estética). Z46.3 = prótese dentária. Z46.4 = aparelho ortodôntico.

## (c) Especialidades odontológicas reconhecidas pelo CFO

Base normativa: **Consolidação das Normas para Procedimentos nos Conselhos de Odontologia, Resolução CFO-63/2005** (título sobre especialidades), com alterações posteriores. **(Não verificado na fonte primária.)** A lista abaixo é a que aparece de forma igual em duas fontes secundárias independentes:

1. Acupuntura
2. Cirurgia e Traumatologia Buco-Maxilo-Faciais (grafia da Res. CFO-63/2005; no SQL foi usada a grafia pedida, "Cirurgia e Traumatologia Bucomaxilofaciais")
3. Dentística
4. Disfunção Temporomandibular e Dor Orofacial
5. Endodontia
6. Estomatologia
7. Harmonização Orofacial (Res. CFO-198/2019, citada pela Res. CFO-SEC-230/2020)
8. Homeopatia
9. Implantodontia
10. Odontogeriatria
11. Odontologia do Esporte
12. Odontologia do Trabalho
13. Odontologia Legal
14. Odontologia para Pacientes com Necessidades Especiais
15. Odontopediatria
16. Ortodontia
17. Ortopedia Funcional dos Maxilares
18. Patologia Oral e Maxilofacial
19. Periodontia
20. Prótese Bucomaxilofacial
21. Prótese Dentária
22. Radiologia Odontológica e Imaginologia
23. Saúde Coletiva

Usadas no catálogo: Cirurgia e Traumatologia Bucomaxilofaciais, Dentística, Disfunção Temporomandibular e Dor Orofacial, Endodontia, Estomatologia, Implantodontia, Odontopediatria, Ortodontia, Ortopedia Funcional dos Maxilares, Periodontia e Prótese Dentária. Também foi usada "Clínica Geral", que não é especialidade.
**Pendente:** confirmar em https://website.cfo.org.br/ (página de especialidades e Consolidação atualizada) se há especialidade nova reconhecida depois de 2019, por exemplo Odontologia Hospitalar, e a grafia oficial atual da Cirurgia.

## (d) Prazo legal de guarda do prontuário odontológico

| Norma | O que diz | Situação |
|---|---|---|
| **Lei 13.787/2018, art. 6º** | "Decorrido o prazo mínimo de 20 (vinte) anos a partir do último registro, os prontuários em suporte de papel e os digitalizados poderão ser eliminados." § 2º: em vez de eliminar, pode devolver ao paciente. § 3º: a eliminação deve resguardar intimidade e sigilo. § 5º: vale para qualquer forma de armazenamento, inclusive microfilme, mídia óptica e documentos nascidos eletrônicos. O art. 1º remete à LGPD (Lei 13.709/2018). | **Confirmado** (texto integral da lei). Primária: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13787.htm |
| **Código de Ética Odontológica, Res. CFO-118/2012** (em vigor desde 01/01/2013; alterado pela Res. CFO-271/2025 apenas nos arts. 20, 32 e 44) | Art. 9º, X: é dever "elaborar e manter atualizados os prontuários na forma das normas em vigor". Art. 17: obrigação de elaborar e manter o prontuário "de forma legível e atualizada" e conservá-lo "em arquivo próprio seja de forma física ou digital"; o parágrafo único manda registrar cada avaliação em ordem cronológica, com data, hora, nome, assinatura e número de inscrição no CRO. Art. 18, I: é infração ética negar ao paciente acesso ao prontuário ou cópia dele. O Código **não fixa prazo** de guarda. | Secundária (dentalpin-web cita o texto). Primária: https://website.cfo.org.br/wp-content/uploads/2018/03/codigo_etica.pdf |
| **Res. CFO-91/2009** (prontuário eletrônico) | Autoriza sistemas informatizados para guarda e manuseio do prontuário. Só dispensa o papel se o sistema cumprir integralmente o **NGS2** do Manual de Certificação SBIS-CFM (art. 3º); com NGS1 o papel não pode ser eliminado (art. 4º); exige assinatura digital ICP-Brasil (art. 5º). Prazos: guarda **permanente** para prontuário em microfilme, mídia óptica ou digitalizado (art. 7º); **10 anos** a partir do último registro para papel não arquivado eletronicamente (art. 8º). | Secundária. Primária: https://transparencia.cfo.org.br/ato-normativo/?id=1360 |
| **Manual do Prontuário do Paciente em Odontologia, CFO, 1ª ed., 2026** e FAQ | Orientações e modelos do CFO sobre prontuário: dados do responsável legal de menores, assinaturas, registros de imagem como prova e prontuário reduzido em urgência. | Secundária. Primária: https://website.cfo.org.br/wp-content/uploads/2026/03/CFO_Manual_do_Prontuario_Ebook_v2.pdf |
| **Res. CFO-SEC-278/2025** (teleodontologia) | O atendimento remoto (teleconsulta, teletriagem etc.) também precisa ser registrado em prontuário. Relevante para triagem por WhatsApp. | Secundária. https://website.cfo.org.br/cfo-regulamenta-a-teleodontologia-e-da-passo-fundamental-para-a-inclusao-de-consultas-odontologicas-no-programa-telessaude-do-sus/ |

**Conclusão prática:** guardar por no mínimo **20 anos a partir do último registro** (Lei 13.787/2018, art. 6º, que é lei federal posterior e prevalece sobre os 10 anos da Res. CFO-91/2009 para papel). Para cópias digitalizadas ou eletrônicas, a postura conservadora é a guarda permanente (Res. CFO-91/2009, art. 7º), ou pelo menos 20 anos. Não foi encontrado parecer posterior do CFO que harmonize expressamente as duas normas. **Pendente:** confirmar na fonte primária.

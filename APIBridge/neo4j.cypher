MATCH (n)
DETACH DELETE n;

UNWIND [
  {name:      'Population', path: 'UF/UF0506/UF0506YDeso/UtbSUNBefDesoRegso', api: 'SCB',
   tableName: 'Befolkning 25-64 år (fr.o.m. 2023, 25–65 år) efter region och utbildningsnivå. År 2015 - 2023'},
  {name:      'Injuries', path: 'skadorochskadehandelserisverigeskommunerochlan/resultat/', api: 'Socialstyrelsen',
   tableName: 'Statistikdatabas för statistik över skador och skadehändelser i Sveriges kommuner och län'},
  {name:      'Households', path: 'BE/BE0101/BE0101S/HushallT05', api: 'SCB',
   tableName: 'Antal hushåll och personer efter region, hushållstyp och antal barn. År 2011-2023'}
] AS tables
MERGE (t:Dataset {name: tables.name, path: tables.path, api: tables.api, tableName: tables.tableName});

MERGE (entity:Municipality {name: 'Municipality', scbCode: 'Region', socialstyrelsenCode: 'region'})
WITH entity
UNWIND [
  {scbCode: '0114', name: 'Upplands Väsby', socialstyrelsenCode: '0114'},
  {scbCode: '0115', name: 'Vallentuna', socialstyrelsenCode: '0115'},
  {scbCode: '0117', name: 'Österåker', socialstyrelsenCode: '0117'},
  {scbCode: '0120', name: 'Värmdö', socialstyrelsenCode: '0120'},
  {scbCode: '0123', name: 'Järfälla', socialstyrelsenCode: '0123'},
  {scbCode: '0125', name: 'Ekerö', socialstyrelsenCode: '0125'},
  {scbCode: '0126', name: 'Huddinge', socialstyrelsenCode: '0126'},
  {scbCode: '0127', name: 'Botkyrka', socialstyrelsenCode: '0127'},
  {scbCode: '0128', name: 'Salem', socialstyrelsenCode: '0128'},
  {scbCode: '0136', name: 'Haninge', socialstyrelsenCode: '0136'},
  {scbCode: '0138', name: 'Tyresö', socialstyrelsenCode: '0138'},
  {scbCode: '0139', name: 'Upplands-Bro', socialstyrelsenCode: '0139'},
  {scbCode: '0140', name: 'Nykvarn', socialstyrelsenCode: '0140'},
  {scbCode: '0160', name: 'Täby', socialstyrelsenCode: '0160'},
  {scbCode: '0162', name: 'Danderyd', socialstyrelsenCode: '0162'},
  {scbCode: '0163', name: 'Sollentuna', socialstyrelsenCode: '0163'},
  {scbCode: '0180', name: 'Stockholm', socialstyrelsenCode: '0180'},
  {scbCode: '0181', name: 'Södertälje', socialstyrelsenCode: '0181'},
  {scbCode: '0182', name: 'Nacka', socialstyrelsenCode: '0182'},
  {scbCode: '0183', name: 'Sundbyberg', socialstyrelsenCode: '0183'},
  {scbCode: '0184', name: 'Solna', socialstyrelsenCode: '0184'},
  {scbCode: '0186', name: 'Lidingö', socialstyrelsenCode: '0186'},
  {scbCode: '0187', name: 'Vaxholm', socialstyrelsenCode: '0187'},
  {scbCode: '0188', name: 'Norrtälje', socialstyrelsenCode: '0188'},
  {scbCode: '0191', name: 'Sigtuna', socialstyrelsenCode: '0191'},
  {scbCode: '0192', name: 'Nynäshamn', socialstyrelsenCode: '0192'}
] AS municipality
MERGE
  (m:Municipality {scbCode: municipality.scbCode, name: municipality.name, socialstyrelsenCode: municipality.
    socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(m);

MERGE (entity:Sex {name: 'Sex', scbCode: 'Kon', socialstyrelsenCode: 'kon'})
WITH entity
UNWIND [
  {name: 'Female', scbCode: '1', socialstyrelsenCode: '1'},
  {name: 'Male', scbCode: '2', socialstyrelsenCode: '2'},
  {name: 'Both sexes', socialstyrelsenCode: '3'}
] AS sex
MERGE (s:Sex {name: sex.name, socialstyrelsenCode: sex.socialstyrelsenCode})
SET s.scbCode = sex.scbCode
MERGE (entity)-[:CONTAINS]->(s);

MERGE (entity:Year {name: 'Year', scbCode: 'Tid'})
FOREACH (year IN range(2011, 2023) |
  MERGE (y:Year {name: toString(year), scbCode: toString(year)})
  MERGE (entity)-[:CONTAINS]->(y)
);

MERGE (entity:YearGroup {name: 'Year Group', socialstyrelsenCode: 'ar'})
WITH entity
UNWIND [
  {name: '1987-1989', socialstyrelsenCode: '1987-1989'},
  {name: '1988-1990', socialstyrelsenCode: '1988-1990'},
  {name: '1989-1991', socialstyrelsenCode: '1989-1991'},
  {name: '1990-1992', socialstyrelsenCode: '1990-1992'},
  {name: '1991-1993', socialstyrelsenCode: '1991-1993'},
  {name: '1992-1994', socialstyrelsenCode: '1992-1994'},
  {name: '1993-1995', socialstyrelsenCode: '1993-1995'},
  {name: '1994-1996', socialstyrelsenCode: '1994-1996'},
  {name: '1995-1997', socialstyrelsenCode: '1995-1997'},
  {name: '1996-1998', socialstyrelsenCode: '1996-1998'},
  {name: '1997-1999', socialstyrelsenCode: '1997-1999'},
  {name: '1998-2000', socialstyrelsenCode: '1998-2000'},
  {name: '1999-2001', socialstyrelsenCode: '1999-2001'},
  {name: '2000-2002', socialstyrelsenCode: '2000-2002'},
  {name: '2001-2003', socialstyrelsenCode: '2001-2003'},
  {name: '2002-2004', socialstyrelsenCode: '2002-2004'},
  {name: '2003-2005', socialstyrelsenCode: '2003-2005'},
  {name: '2004-2006', socialstyrelsenCode: '2004-2006'},
  {name: '2005-2007', socialstyrelsenCode: '2005-2007'},
  {name: '2006-2008', socialstyrelsenCode: '2006-2008'},
  {name: '2007-2009', socialstyrelsenCode: '2007-2009'},
  {name: '2008-2010', socialstyrelsenCode: '2008-2010'},
  {name: '2009-2011', socialstyrelsenCode: '2009-2011'},
  {name: '2010-2012', socialstyrelsenCode: '2010-2012'},
  {name: '2011-2013', socialstyrelsenCode: '2011-2013'},
  {name: '2012-2014', socialstyrelsenCode: '2012-2014'},
  {name: '2013-2015', socialstyrelsenCode: '2013-2015'},
  {name: '2014-2016', socialstyrelsenCode: '2014-2016'},
  {name: '2015-2017', socialstyrelsenCode: '2015-2017'},
  {name: '2016-2018', socialstyrelsenCode: '2016-2018'},
  {name: '2017-2019', socialstyrelsenCode: '2017-2019'},
  {name: '2018-2020', socialstyrelsenCode: '2018-2020'},
  {name: '2019-2021', socialstyrelsenCode: '2019-2021'},
  {name: '2020-2022', socialstyrelsenCode: '2020-2022'},
  {name: '2021-2023', socialstyrelsenCode: '2021-2023'}
] AS yearGroup
MERGE (y:YearGroup {name: yearGroup.name, socialstyrelsenCode: yearGroup.socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(y);

MERGE (entity:Age {name: 'Age', scbCode: 'Alder'})
FOREACH (i IN range(0, 99) |
  MERGE (age:Age {name: toString(i), scbCode: toString(i)})
  MERGE (entity)-[:CONTAINS]->(age)
)
MERGE (age100:Age {name: '100+', scbCode: '100+'})
MERGE (entity)-[:CONTAINS]->(age100);

MERGE (entity:AgeGroup {name: 'Age Group', socialstyrelsenCode: 'alder'})
WITH entity
UNWIND [
  {name: '0-14', socialstyrelsenCode: '1'},
  {name: '15-24', socialstyrelsenCode: '2'},
  {name: '25-44', socialstyrelsenCode: '3'},
  {name: '45-64', socialstyrelsenCode: '4'},
  {name: '65-79', socialstyrelsenCode: '5'},
  {name: '80+', socialstyrelsenCode: '6'},
  {name: 'All ages', socialstyrelsenCode: '9'}
] AS ageGroup
MERGE (a:AgeGroup {name: ageGroup.name, socialstyrelsenCode: ageGroup.socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(a);

MERGE (entity:EducationLevel {name: 'Education Level', scbCode: 'UtbildningsNiva'})
WITH entity
UNWIND [
  {name: 'Primary and lower secondary education', scbCode: '21'},
  {name: 'Upper secondary education', scbCode: '3+4'},
  {name: 'Post-secondary education, less than 3 years', scbCode: '5'},
  {name: 'Post-secondary education 3 years or more', scbCode: '6'},
  {name: 'No information about level of educational attainment', scbCode: 'US'}
] AS educationLevel
MERGE (e:EducationLevel {name: educationLevel.name, scbCode: educationLevel.scbCode})
MERGE (entity)-[:CONTAINS]->(e);

MERGE (entity:CareType {name: 'Care Type', socialstyrelsenCode: 'vardform'})
WITH entity
UNWIND [
  {name: 'Inpatient care only', socialstyrelsenCode: 'SV'},
  {name: 'Inpatient and/or specialized outpatient care', socialstyrelsenCode: 'SVOV'}
] AS careType
MERGE (c:CareType {name: careType.name, socialstyrelsenCode: careType.socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(c);

MERGE (entity:TypeOfAccident {name: 'Type of Accident', socialstyrelsenCode: 'typ'})
WITH entity
UNWIND [
  {name: 'Road transport accident (including riding accident)', socialstyrelsenCode: '1'},
  {name: 'Fall accident', socialstyrelsenCode: '2'},
  {name: 'Other accident', socialstyrelsenCode: '3'},
  {name: 'Intentionally inflicted injury', socialstyrelsenCode: '4'},
  {name: 'Concussion', socialstyrelsenCode: '5'},
  {name: 'Hip fracture', socialstyrelsenCode: '6'},
  {name: 'Total number of injured', socialstyrelsenCode: '9'}
] AS type
MERGE (t:TypeOfAccident {name: type.name, socialstyrelsenCode: type.socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(t);

MERGE (entity:Measurement {name: 'Measurement', socialstyrelsenCode: 'matt'})
WITH entity
UNWIND [
  {name: 'Number of treated persons (3-year average)', socialstyrelsenCode: '1'},
  {name: 'Number of treated persons per 100,000 inhabitants (3-year average)', socialstyrelsenCode: '2'}
] AS measurement
MERGE (m:Measurement {name: measurement.name, socialstyrelsenCode: measurement.socialstyrelsenCode})
MERGE (entity)-[:CONTAINS]->(m);

MERGE (entity:HouseholdType {name: 'Household Type', scbCode: 'Hushallstyp'})
WITH entity
UNWIND [
  {name: 'Single without children', scbCode: 'ESUB'},
  {name: 'Single with children aged 0-24', scbCode: 'ESMB25'},
  {name: 'Single with children aged 25 and older', scbCode: 'ESMB24'},
  {name: 'Cohabiting/married without children', scbCode: 'SMUB'},
  {name: 'Cohabiting with children aged 0-24', scbCode: 'SBMB25'},
  {name: 'Cohabiting with children aged 25 and older', scbCode: 'SBMB24'},
  {name: 'Other household without children', scbCode: 'OVRIUB'},
  {name: 'Other household with children aged 0-24', scbCode: 'ÖMB25'},
  {name: 'Other household with children aged 25 and older', scbCode: 'ÖMB24'},
  {name: 'No information', scbCode: 'SAKNAS'}
] AS household
MERGE (h:HouseholdType {name: household.name, scbCode: household.scbCode})
MERGE (entity)-[:CONTAINS]->(h);

MERGE (entity:Children {name: 'Children', scbCode: 'Barn'})
WITH entity
UNWIND [
  {name: '0 children', scbCode: 'UB'},
  {name: '1 child', scbCode: 'M1B'},
  {name: '2 children', scbCode: 'M2B'},
  {name: '3 children or more', scbCode: 'M3+B'},
  {name: 'No information', scbCode: 'SAKNAS'}
] AS children
MERGE (c:Children {name: children.name, scbCode: children.scbCode})
MERGE (entity)-[:CONTAINS]->(c);

MATCH (a:Dataset {name: 'Population'})
UNWIND ['Municipality', 'Education Level', 'Year'] AS metadata
MATCH (b {name: metadata})
MERGE (a)-[:CONTAINS]->(b);

MATCH (a:Dataset {name: 'Injuries'})
UNWIND ['Care Type', 'Municipality', 'Type of Accident', 'Age Group', 'Sex', 'Year Group', 'Measurement'] AS metadata
MATCH (b {name: metadata})
MERGE (a)-[:CONTAINS]->(b);

MATCH (a:Dataset {name: 'Households'})
UNWIND ['Municipality', 'Household Type', 'Children', 'Year'] AS metadata
MATCH (b {name: metadata})
MERGE (a)-[:CONTAINS]->(b);
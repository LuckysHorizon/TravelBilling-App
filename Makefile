.PHONY: dev dev-backend dev-frontend dev-pdf test lint build clean logs deploy-frontend deploy-backend deploy-pdf

dev:
	docker-compose up --build

dev-backend:
	cd backend && ./mvnw spring-boot:run

dev-frontend:
	cd frontend && npm run dev

dev-pdf:
	cd pdf-extractor && uvicorn main:app --reload --port 8000

test:
	cd frontend && npm run build
	cd backend && ./mvnw test
	cd pdf-extractor && pytest || true

lint:
	cd frontend && npm run build
	cd backend && ./mvnw -q -DskipTests compile
	cd pdf-extractor && flake8 . || true

build:
	docker-compose build

clean:
	docker-compose down -v --remove-orphans

logs:
	docker-compose logs -f

deploy-frontend:
	cd frontend && npx vercel --prod

deploy-backend:
	curl -X POST $$RENDER_DEPLOY_HOOK_URL

deploy-pdf:
	railway up --service pdf-extractor
